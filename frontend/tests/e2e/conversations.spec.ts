import { test, expect, type Page, type Route } from "@playwright/test";
import { join } from "node:path";
import type { ConversationResponse } from "../../src/types/conversation";

const reservation = {
    id: "reservation-12345678", unitId: "unit-12345678", unitName: "Apartament nad Wisłą",
    guestName: "Guest", userId: "guest", startDate: "2026-10-01", endDate: "2026-10-08",
    status: "CONFIRMED", createdAt: "2026-09-01T10:00:00Z",
};
const conversation: ConversationResponse = {
    id: "conversation-1", reservationId: reservation.id, guestUserId: "guest", ownerUserId: "owner",
    reservationStatus: "CONFIRMED", createdAt: "2026-10-01T10:00:00Z",
    lastMessageAt: "2026-10-30T10:00:00Z", retentionExpiresOn: null,
};

async function respond(route: Route, json: unknown, status = 200) {
    await route.fulfill({ status, json });
}

function authToken(roles: string[], userId: string, firstName = "Test") {
    return `header.${Buffer.from(JSON.stringify({ userId, role: roles, firstName })).toString("base64url")}.signature`;
}

async function setup(page: Page, roles: string[] = ["ROLE_GUEST"], language = "en", userId = roles.includes("ROLE_OWNER") ? "owner" : roles.includes("ROLE_ADMIN") ? "admin" : "guest") {
    // Inbox/navigation regressions exercise the REST fallback; live STOMP has its own broker fixture.
    await page.routeWebSocket("**/ws/conversations", socket => socket.close({ code: 1011, reason: "Test live transport unavailable" }));
    const token = authToken(roles, userId);
    await page.addInitScript(({ token, language, loggedIn }) => {
        if (loggedIn) localStorage.setItem("token", token);
        else localStorage.removeItem("token");
        localStorage.setItem("language", language);
    }, { token, language, loggedIn: roles.length > 0 });
    await page.route("**/api/**", async route => {
        const path = new URL(route.request().url()).pathname;
        if (path === "/api/v1/conversations") return respond(route, [conversation]);
        if (path.endsWith("/messages")) return respond(route, { items: [], hasMore: false, nextCursor: null });
        if (path.startsWith("/api/v1/conversations/")) return respond(route, conversation);
        if (path.endsWith("/conversation")) return respond(route, conversation);
        if (path === "/api/v1/reservations/my" || path === "/api/v1/admin/reservations") return respond(route, [reservation]);
        if (path.startsWith("/api/v1/reservations/")) return respond(route, reservation);
        if (path.startsWith("/api/billing/settlements/")) return respond(route, { settlement: { id: "settlement", status: "PENDING" }, items: [] });
        if (path.endsWith("/settlement-items")) return respond(route, []);
        if (path.includes("/units/")) return respond(route, { capacity: 2 });
        return respond(route, []);
    });
}

async function screenshot(page: Page, name: string, project: string) {
    if (process.env.KWATERA_SCREENSHOT_DIR) {
        await page.screenshot({ path: join(process.env.KWATERA_SCREENSHOT_DIR, `${name}-${project}.png`), fullPage: false });
    }
}

for (const language of ["en", "pl"]) {
    test(`message translation and original toggle (${language})`, async ({ page }) => {
        await setup(page, ["ROLE_GUEST"], language);
        await page.route(/\/api\/v1\/conversations\/[^/]+\/messages(?:\?.*)?$/, route => respond(route, {
            items: [
                { id: "message-1", conversationId: conversation.id, senderUserId: "owner", content: "Bonjour !", createdAt: "2026-10-01T10:00:00Z" },
                { id: "message-2", conversationId: conversation.id, senderUserId: "guest", content: "My message", createdAt: "2026-10-01T10:01:00Z" },
            ],
            hasMore: false, nextCursor: null,
        }));
        let calls = 0;
        await page.route("**/messages/message-1/translation", async route => {
            calls++;
            expect(route.request().method()).toBe("POST");
            expect(route.request().postDataJSON()).toEqual({ targetLanguage: language });
            expect(route.request().headers().authorization).toContain("Bearer ");
            await respond(route, { translatedText: language === "pl" ? "Dzień dobry!" : "Hello!", sourceLanguage: "auto", targetLanguage: language });
        });
        await page.goto(`/conversations/${conversation.id}`);
        const translate = page.getByRole("button", { name: language === "pl" ? "Przetłumacz na polski" : "Translate to English", exact: true });
        await expect(page.getByText("Bonjour !", { exact: true })).toBeVisible();
        const ownMessage = page.getByRole("article").filter({ hasText: "My message" });
        await expect(ownMessage).toBeVisible();
        await expect(ownMessage.getByRole("button")).toHaveCount(0);
        await translate.click();
        await expect(page.getByText(language === "pl" ? "Dzień dobry!" : "Hello!", { exact: true })).toBeVisible();
        await expect(page.getByText(/check the original for accuracy|sprawdź poprawność z oryginałem/i)).toHaveCount(0);
        await page.getByRole("button", { name: language === "pl" ? "Pokaż oryginał" : "Show original", exact: true }).click();
        await expect(page.getByText("Bonjour !", { exact: true })).toBeVisible();
        await translate.click();
        expect(calls).toBe(1);
    });
}

test("AI outage keeps original and composer usable and translation can retry", async ({ page }) => {
    await setup(page);
    await page.route(/\/api\/v1\/conversations\/[^/]+\/messages(?:\?.*)?$/, route => respond(route, {
        items: [{ id: "message-1", conversationId: conversation.id, senderUserId: "owner", content: "Bonjour !", createdAt: "2026-10-01T10:00:00Z" }],
        hasMore: false, nextCursor: null,
    }));
    let release!: () => void;
    const gate = new Promise<void>(resolve => { release = resolve; });
    let failed = false;
    await page.route("**/messages/message-1/translation", async route => {
        if (!failed) { await gate; failed = true; await respond(route, {}, 503); }
        else await respond(route, { translatedText: "Hello!", sourceLanguage: "auto", targetLanguage: "en" });
    });
    await page.goto(`/conversations/${conversation.id}`);
    await page.getByRole("button", { name: "Translate to English", exact: true }).click();
    await expect(page.getByRole("button", { name: "Translating…", exact: true })).toBeDisabled();
    await page.locator("#chat-message").fill("My draft survives");
    await expect(page.getByRole("button", { name: "Send", exact: true })).toBeEnabled();
    release();
    await expect(page.getByRole("alert")).toContainText("Translation unavailable");
    await expect(page.getByText("Bonjour !", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Translate to English", exact: true }).click();
    await expect(page.getByText("Hello!", { exact: true })).toBeVisible();
    await expect(page.locator("#chat-message")).toHaveValue("My draft survives");
});

for (const [label, roles, visible] of [
    ["guest", ["ROLE_GUEST"], true], ["owner", ["ROLE_OWNER"], true],
    ["administrator", ["ROLE_ADMIN"], true], ["signed out", [], false],
    ["owner with administrator role", ["ROLE_ADMIN", "ROLE_OWNER"], true],
] as const) {
    test(`navigation visibility: ${label}`, async ({ page }) => {
        await setup(page, [...roles]);
        await page.goto("/about");
        if (roles.length) await page.getByRole("button", { name: "My Account", exact: true }).click();
        await expect(page.getByRole("link", { name: "Messages", exact: true })).toHaveCount(visible ? 1 : 0);
        if (visible) {
            await page.getByRole("link", { name: "Messages", exact: true }).click();
            await expect(page).toHaveURL(/\/conversations$/);
            await expect(page.getByRole("heading", { name: "Messages", exact: true })).toBeVisible();
        }
    });
}

test("inbox sorts participant conversations and links chat, reservation and inbox", async ({ page }, info) => {
    await setup(page);
    const errors: string[] = [];
    page.on("pageerror", error => errors.push(error.message));
    page.on("console", message => { if (message.type() === "error") errors.push(message.text()); });
    await page.route("**/api/v1/conversations", route => respond(route, [
        { ...conversation, id: "older", reservationId: "older-87654321", lastMessageAt: null },
        conversation,
        { ...conversation, id: "other", guestUserId: "unrelated", ownerUserId: "unrelated" },
    ]));
    await page.goto("/conversations");
    await expect(page).toHaveTitle(/Kwatera/i);
    const list = page.getByRole("list", { name: "Messages", exact: true });
    await expect(list.getByRole("listitem")).toHaveCount(2);
    await expect(list.getByRole("link").first()).toHaveAttribute("href", "/conversations/conversation-1");
    await expect(list.getByRole("heading").first()).toHaveText(reservation.unitName);
    await expect(page.locator("vite-error-overlay")).toHaveCount(0);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await screenshot(page, "inbox", info.project.name);
    await list.getByRole("link").first().click();
    await expect(page).toHaveURL("/conversations/conversation-1");
    await expect(page.getByRole("heading", { name: "Reservation conversation" })).toBeVisible();
    await page.getByRole("link", { name: "Back to reservation", exact: true }).click();
    await expect(page).toHaveURL(`/reservations/${reservation.id}`);
    await page.getByRole("button", { name: "Open conversation", exact: true }).click();
    await expect(page).toHaveURL("/conversations/conversation-1");
    await page.getByRole("link", { name: "Back to messages", exact: true }).click();
    await expect(list.getByRole("listitem")).toHaveCount(2);
    expect(errors).toEqual([]);
});

test("empty and loading states", async ({ page }) => {
    await setup(page);
    let release!: () => void;
    const gate = new Promise<void>(resolve => { release = resolve; });
    await page.route("**/api/v1/conversations", async route => { await gate; await respond(route, []); });
    await page.goto("/conversations");
    await expect(page.getByRole("status")).toHaveText("Loading conversations…");
    release();
    await expect(page.getByRole("heading", { name: "No conversations yet" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Open conversation" })).toHaveCount(0);
});

test("failed loading can be retried without stale data", async ({ page }) => {
    await setup(page);
    let failing = true;
    await page.route("**/api/v1/conversations", route => respond(route, failing ? {} : [conversation], failing ? 503 : 200));
    await page.goto("/conversations");
    await expect(page.getByRole("alert")).toHaveText("Conversations could not be loaded. Please try again.");
    failing = false;
    await page.getByRole("button", { name: "Try again" }).click();
    await expect(page.getByRole("link", { name: "Open conversation" })).toBeVisible();
});

for (const status of [401, 403, 404, 409, 410]) {
    test(`inbox handles HTTP ${status} without conversation data`, async ({ page }) => {
        await setup(page);
        await page.route("**/api/v1/conversations", route => respond(route, { message: "private backend detail" }, status));
        await page.goto("/conversations");
        await expect(page.getByRole("alert")).toContainText(status === 401 ? "session" : status < 409 ? "access" : "retention");
        await expect(page.getByText("private backend detail")).toHaveCount(0);
        await expect(page.getByRole("link", { name: "Open conversation" })).toHaveCount(0);
        if (status === 401) await expect(page.getByRole("main").getByRole("link", { name: "Log in", exact: true })).toHaveAttribute("href", "/login");
    });
}

for (const [route, role] of [["/my-reservations", "ROLE_GUEST"], ["/admin/reservations", "ROLE_OWNER"], [`/reservations/${reservation.id}`, "ROLE_GUEST"]]) {
    test(`reservation entry reuses PUT and prevents duplicate clicks: ${route}`, async ({ page }) => {
        await setup(page, [role]);
        let calls = 0;
        let release!: () => void;
        const gate = new Promise<void>(resolve => { release = resolve; });
        await page.route("**/api/v1/reservations/*/conversation", async request => {
            calls++;
            expect(request.request().method()).toBe("PUT");
            expect(request.request().headers().authorization).toContain("Bearer ");
            await gate;
            await respond(request, conversation);
        });
        await page.goto(route);
        const open = page.getByRole("button", { name: "Open conversation", exact: true });
        await expect(open).toBeVisible();
        await open.evaluate(button => {
            if (!(button instanceof HTMLButtonElement)) throw new Error("Expected a conversation button");
            button.click(); button.click(); button.click();
        });
        await expect(page.getByRole("button", { name: "Opening conversation…" })).toBeDisabled();
        await expect.poll(() => calls).toBe(1);
        release();
        await expect(page).toHaveURL("/conversations/conversation-1");
        expect(calls).toBe(1);
    });
}

for (const status of [401, 404, 409, 500]) {
    test(`reservation entry handles HTTP ${status} and allows retry`, async ({ page }) => {
        await setup(page);
        let failing = true;
        await page.route("**/api/v1/reservations/*/conversation", route => respond(route, failing ? {} : conversation, failing ? status : 200));
        await page.goto(`/reservations/${reservation.id}`);
        await page.getByRole("button", { name: "Open conversation" }).click();
        await expect(page.getByRole("alert")).toContainText(status === 401 ? "session" : status === 404 ? "access" : status === 409 ? "retention" : "could not be opened");
        await expect(page).toHaveURL(`/reservations/${reservation.id}`);
        failing = false;
        await page.getByRole("button", { name: "Open conversation" }).click();
        await expect(page).toHaveURL("/conversations/conversation-1");
    });
}

test("unrelated administrator has no reservation actions and backend denies direct conversation access", async ({ page }) => {
    await setup(page, ["ROLE_ADMIN"]);
    let entryCalls = 0;
    page.on("request", request => { if (request.url().endsWith("/conversation")) entryCalls++; });
    await page.route("**/api/v1/conversations", route => respond(route, []));
    await page.route("**/api/v1/conversations/conversation-1", route => respond(route, {}, 404));
    for (const path of ["/admin/reservations", `/reservations/${reservation.id}`]) {
        await page.goto(path);
        await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
        await expect(page.getByRole("button", { name: "Open conversation" })).toHaveCount(0);
    }
    await page.goto("/conversations");
    await expect(page.getByRole("heading", { name: "No conversations yet" })).toBeVisible();
    await page.goto("/conversations/conversation-1");
    await expect(page).toHaveURL("/conversations/conversation-1");
    await expect(page.getByRole("alert")).toContainText("do not have access");
    await expect(page.getByText(reservation.unitName)).toHaveCount(0);
    await expect(page.getByRole("textbox")).toHaveCount(0);
    expect(entryCalls).toBe(0);
});

test("signed-out deep links go to login", async ({ page }) => {
    await setup(page, []);
    for (const path of ["/conversations", "/conversations/conversation-1"]) {
        await page.goto(path);
        await expect(page).toHaveURL("/login");
    }
});

test("blocked reservation cannot request a conversation", async ({ page }) => {
    await setup(page);
    await page.route("**/api/v1/reservations/reservation-12345678", route => respond(route, { ...reservation, status: "BLOCKED" }));
    await page.goto(`/reservations/${reservation.id}`);
    await expect(page.getByRole("button", { name: "Open conversation" })).toBeDisabled();
    await expect(page.getByRole("status")).toContainText("retention");
});

test("context enrichment stays on the visible page with at most three concurrent requests", async ({ page }) => {
    await setup(page);
    const requested: string[] = [];
    let active = 0;
    let peak = 0;
    await page.route("**/api/v1/conversations", route => respond(route, Array.from({ length: 25 }, (_, index) => ({
        ...conversation, id: `conversation-${String(index).padStart(2, "0")}`, reservationId: `reservation-${String(index).padStart(8, "0")}`,
    }))));
    await page.route("**/api/v1/reservations/*", async route => {
        requested.push(route.request().url());
        active++;
        peak = Math.max(peak, active);
        await new Promise(resolve => setTimeout(resolve, 30));
        active--;
        await respond(route, {} , 404);
    });
    await page.goto("/conversations");
    await expect(page.getByRole("list", { name: "Messages", exact: true }).getByRole("listitem")).toHaveCount(10);
    await expect.poll(() => requested.length).toBe(10);
    expect(peak).toBeLessThanOrEqual(3);
    await expect.poll(() => active).toBe(0);
    await expect(page.getByRole("heading", { level: 2 }).first()).toHaveText("Property name not available");
    await page.getByRole("button", { name: "Next", exact: true }).click();
    await expect(page.getByRole("status")).toHaveText("Page 2 of 3");
    await expect.poll(() => requested.length).toBe(20);
    expect(peak).toBeLessThanOrEqual(3);
});

test("Polish inbox and date fallback", async ({ page }, info) => {
    await setup(page, ["ROLE_OWNER"], "pl");
    await page.route("**/api/v1/conversations", route => respond(route, [{ ...conversation, lastMessageAt: null, createdAt: null }]));
    await page.goto("/conversations");
    await expect(page.getByRole("heading", { name: "Wiadomości", exact: true })).toBeVisible();
    await expect(page.getByText("Ostatnia aktywność: Data niedostępna")).toBeVisible();
    await expect(page.getByRole("link", { name: "Otwórz rozmowę" })).toBeVisible();
    await expect(page.getByRole("heading", { name: reservation.unitName })).toBeVisible();
    await screenshot(page, "inbox-pl", info.project.name);
});

test("expired direct conversation hides context and returns to inbox", async ({ page }) => {
    await setup(page);
    await page.route("**/api/v1/conversations/conversation-1", route => respond(route, {}, 409));
    await page.goto("/conversations/conversation-1");
    await expect(page.getByRole("alert")).toContainText("retention");
    await expect(page.getByText(reservation.unitName)).toHaveCount(0);
    await expect(page.getByRole("textbox")).toHaveCount(0);
    await page.getByRole("link", { name: "Back to messages" }).click();
    await expect(page).toHaveURL("/conversations");
});

test.describe("review regressions", () => {
    test("administrator participants can use the inbox, direct links and their own reservation action", async ({ page }) => {
        await setup(page, ["ROLE_ADMIN"], "en", "guest");
        await page.goto("/conversations");
        await page.getByRole("link", { name: "Open conversation" }).click();
        await expect(page.getByRole("heading", { name: "Reservation conversation" })).toBeVisible();
        await page.goto("/conversations/conversation-1");
        await expect(page.getByRole("textbox")).toBeVisible();
        for (const path of ["/admin/reservations", `/reservations/${reservation.id}`]) {
            await page.goto(path);
            await page.getByRole("button", { name: "Open conversation", exact: true }).click();
            await expect(page).toHaveURL("/conversations/conversation-1");
        }
    });

    test("administrator owner snapshots retain conversation access without assuming current ownership", async ({ page }) => {
        await setup(page, ["ROLE_ADMIN"], "en", "owner");
        await page.goto("/conversations");
        await page.getByRole("link", { name: "Open conversation" }).click();
        await expect(page.getByRole("textbox")).toBeVisible();
    });

    test("mixed administrator roles do not expose actions for unrelated reservations", async ({ page }) => {
        await setup(page, ["ROLE_ADMIN", "ROLE_OWNER"]);
        await page.goto("/admin/reservations");
        await expect(page.getByRole("link", { name: /^view details$/i })).toBeVisible();
        await expect(page.getByRole("button", { name: "Open conversation" })).toHaveCount(0);
    });

    test("cross-tab account changes clear loaded metadata and ignore old context responses", async ({ page, context }) => {
        await setup(page);
        let releaseOld!: () => void;
        let releaseNew!: () => void;
        const oldGate = new Promise<void>(resolve => { releaseOld = resolve; });
        const newGate = new Promise<void>(resolve => { releaseNew = resolve; });
        let oldContextStarted = false;
        await page.route("**/api/v1/reservations/reservation-12345678", async route => {
            oldContextStarted = true;
            await oldGate;
            await respond(route, { ...reservation, unitName: "Private old account context" });
        });
        const newToken = authToken(["ROLE_GUEST"], "new-guest");
        await page.route("**/api/v1/conversations", async route => {
            if (route.request().headers().authorization === `Bearer ${newToken}`) {
                await newGate;
                return respond(route, []);
            }
            return respond(route, [conversation]);
        });
        await page.goto("/conversations");
        await expect(page.getByText("#RES-12345678")).toBeVisible();
        await expect.poll(() => oldContextStarted).toBe(true);
        const otherTab = await context.newPage();
        await otherTab.goto("/about");
        await otherTab.evaluate(token => localStorage.setItem("token", token), newToken);
        await expect(page.getByText("#RES-12345678")).toHaveCount(0);
        await expect(page.getByRole("status")).toHaveText("Loading conversations…");
        releaseOld();
        releaseNew();
        await expect(page.getByRole("heading", { name: "No conversations yet" })).toBeVisible();
        await expect(page.getByText("Private old account context")).toHaveCount(0);
        await expect(page.getByRole("link", { name: "Open conversation" })).toHaveCount(0);
        await otherTab.close();
    });

    test("in-flight inbox results from a previous token cannot overwrite a renewed session", async ({ page, context }) => {
        await setup(page);
        let releaseOld!: () => void;
        const oldGate = new Promise<void>(resolve => { releaseOld = resolve; });
        let oldStarted = false;
        let oldFinished = false;
        const newToken = authToken(["ROLE_GUEST"], "guest", "Renewed session");
        await page.route("**/api/v1/conversations", async route => {
            if (route.request().headers().authorization === `Bearer ${newToken}`) return respond(route, []);
            oldStarted = true;
            await oldGate;
            await respond(route, [conversation]);
            oldFinished = true;
        });
        await page.goto("/conversations");
        await expect.poll(() => oldStarted).toBe(true);
        const otherTab = await context.newPage();
        await otherTab.goto("/about");
        await otherTab.evaluate(token => localStorage.setItem("token", token), newToken);
        await expect(page.getByRole("heading", { name: "No conversations yet" })).toBeVisible();
        releaseOld();
        await expect.poll(() => oldFinished).toBe(true);
        await expect(page.getByRole("heading", { name: "No conversations yet" })).toBeVisible();
        await expect(page.getByText("#RES-12345678")).toHaveCount(0);
        await otherTab.close();
    });

    for (const clearAll of [false, true]) {
        test(`cross-tab ${clearAll ? "storage clear" : "logout"} removes inbox data`, async ({ page, context }) => {
            await setup(page);
            await page.goto("/conversations");
            await expect(page.getByRole("heading", { name: reservation.unitName })).toBeVisible();
            const otherTab = await context.newPage();
            await otherTab.goto("/about");
            await otherTab.evaluate(clearAll => {
                if (clearAll) localStorage.clear();
                else localStorage.removeItem("token");
            }, clearAll);
            await expect(page).toHaveURL("/login");
            await expect(page.getByText(reservation.unitName)).toHaveCount(0);
            await expect(page.getByText("#RES-12345678")).toHaveCount(0);
            await otherTab.close();
        });
    }

    for (const [language, currency, price] of [["en", "USD", 987654321.99], ["pl", "EUR", 1234567.89], ["pl", "PLN", 9999.99]] as const) {
        test(`reservation card layout: ${language} ${currency}`, async ({ page }, info) => {
            await setup(page, ["ROLE_GUEST"], language);
            const errors: string[] = [];
            page.on("pageerror", error => errors.push(error.message));
            page.on("console", message => { if (message.type() === "error") errors.push(message.text()); });
            await page.route("**/api/v1/reservations/my", route => respond(route, [{
                ...reservation, convertedTotalPrice: price,
                currencyInfo: { displayCurrency: currency, baseCurrency: "PLN", exchangeRate: 1, rateEffectiveDate: "2026-10-01" },
            }]));
            await page.route("**/api/billing/settlements/*", route => respond(route, { settlement: { id: "settlement", invoiceRequested: true, status: "PAID" }, items: [] }));
            await page.goto("/my-reservations");
            await expect(page).toHaveTitle(/KWATERA/);
            const card = page.getByRole("article");
            await expect(card.getByRole("heading", { name: reservation.unitName })).toBeVisible();
            const dates = card.locator("time").first().locator("..");
            const total = card.getByText(`${price.toFixed(2)} ${currency}`, { exact: true });
            const status = card.getByText(language === "en" ? "Confirmed" : "Potwierdzona", { exact: true });
            const actions = card.getByRole("link", { name: language === "en" ? "View Details" : "Szczegóły", exact: true }).locator("..");
            for (const width of [320, 390, 768, 1024, 1280, 1440]) {
                await page.setViewportSize({ width, height: 1000 });
                await expect(dates).toBeVisible();
                await expect(total).toBeVisible();
                await expect(card.getByRole("button", { name: language === "en" ? "Open conversation" : "Otwórz rozmowę", exact: true })).toBeVisible();
                const bounds = await Promise.all([dates, total, status, actions].map(locator => locator.boundingBox()));
                for (let a = 0; a < bounds.length; a++) {
                    for (let b = a + 1; b < bounds.length; b++) {
                        const first = bounds[a]!;
                        const second = bounds[b]!;
                        expect(first.x + first.width <= second.x + 1 || second.x + second.width <= first.x + 1 || first.y + first.height <= second.y + 1 || second.y + second.height <= first.y + 1, `overlap at ${width}px between blocks ${a} and ${b}`).toBe(true);
                    }
                }
                expect(await card.evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true);
                expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `page overflow at ${width}px`).toBe(true);
                expect(await total.evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true);
                if (width === 320 || width === 1024 || width === 1280) await screenshot(page, `reservation-${language}-${currency}-${width}`, info.project.name);
            }
            await expect(page.locator("vite-error-overlay")).toHaveCount(0);
            expect(errors).toEqual([]);
        });
    }
});
