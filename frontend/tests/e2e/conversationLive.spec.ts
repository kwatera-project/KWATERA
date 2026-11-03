import { test, expect, type Page, type Route, type WebSocketRoute } from "@playwright/test";
import { join } from "node:path";

// Controlled broker fixture only: the application itself uses the real @stomp/stompjs parser/client.
const conversationId = "11111111-1111-1111-1111-111111111111";
const conversation = { id: conversationId, reservationId: "reservation", guestUserId: "guest", ownerUserId: "owner",
    reservationStatus: "CONFIRMED", createdAt: "2026-10-01T10:00:00Z", lastMessageAt: null, retentionExpiresOn: null };
const message = (id: number, content = `Message ${id}`) => ({ id: String(id).padStart(8, "0"), conversationId,
    senderUserId: id % 2 ? "guest" : "owner", content, createdAt: new Date(Date.UTC(2026, 9, 28, 10, 0, id)).toISOString() });
const historyPage = (items: ReturnType<typeof message>[], nextCursor: string | null = null) => ({ items, nextCursor, hasMore: nextCursor !== null });
const token = (userId = "guest") => `header.${Buffer.from(JSON.stringify({ userId, role: [userId === "owner" ? "ROLE_OWNER" : "ROLE_GUEST"], exp: Math.floor(Date.now() / 1000) + 3600 })).toString("base64url")}.signature`;
const frame = (command: string, headers: Record<string, string>, body = "") => `${command}\n${Object.entries(headers).map(([key, value]) => `${key}:${value}`).join("\n")}\n\n${body}\0`;
const flushPage = (page: Page) => page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => resolve())));

async function setup(page: Page, options: { manualReceipt?: boolean; rejectTransport?: boolean; language?: string; userId?: string } = {}) {
    const jwt = token(options.userId);
    await page.addInitScript(({ jwt, language }) => { localStorage.setItem("token", jwt); localStorage.setItem("language", language); }, { jwt, language: options.language ?? "en" });
    await page.route("**/api/**", async route => {
        const path = new URL(route.request().url()).pathname;
        if (path.endsWith("/messages")) return route.fulfill({ json: historyPage([message(1)]) });
        if (path === "/api/v1/conversations") return route.fulfill({ json: [conversation] });
        if (path.startsWith("/api/v1/conversations/")) return route.fulfill({ json: { ...conversation, id: path.split("/").at(-1) } });
        if (path.startsWith("/api/v1/reservations/")) return route.fulfill({ json: { id: "reservation", unitName: "Live test stay", startDate: "2026-10-01", endDate: "2026-10-08" } });
        return route.fulfill({ json: [] });
    });
    const sockets: { ws: WebSocketRoute; subscriptions: Map<string, string>; receipt: string; closed: boolean }[] = [];
    const commands: { command: string; headers: Record<string, string>; url: string }[] = [];
    let sequence = 0;
    await page.routeWebSocket("**/ws/conversations", ws => {
        const socket = { ws, subscriptions: new Map<string, string>(), receipt: "", closed: false };
        sockets.push(socket);
        ws.onClose(() => { socket.closed = true; });
        ws.onMessage(data => {
            const text = String(data);
            const [head] = text.split("\n\n");
            const [command, ...lines] = head.split("\n");
            if (!command.trim()) return;
            const headers = Object.fromEntries(lines.filter(Boolean).map(line => { const index = line.indexOf(":"); return [line.slice(0, index), line.slice(index + 1)]; }));
            commands.push({ command, headers, url: ws.url() });
            if (command === "CONNECT") {
                if (options.rejectTransport) ws.close({ code: 1011, reason: "Test transport failure" });
                else ws.send(frame("CONNECTED", { version: "1.2", "heart-beat": "0,0" }));
            }
            if (command === "SUBSCRIBE") {
                socket.subscriptions.set(headers.destination, headers.id);
                if (headers.receipt) {
                    socket.receipt = headers.receipt;
                    if (!options.manualReceipt) ws.send(frame("RECEIPT", { "receipt-id": headers.receipt }));
                }
            }
        });
    });
    return {
        jwt, sockets, commands,
        async subscribed(count = 1) { await expect.poll(() => sockets.filter(s => s.receipt).length).toBe(count); },
        receipt(id?: string) { const socket = sockets.at(-1)!; socket.ws.send(frame("RECEIPT", { "receipt-id": id ?? socket.receipt })); },
        deliver(value: ReturnType<typeof message>, targetConversation = conversationId) {
            const socket = sockets.at(-1)!;
            const destination = `/user/queue/conversations/${targetConversation}/messages`;
            socket.ws.send(frame("MESSAGE", { subscription: socket.subscriptions.get(destination)!, destination, "message-id": String(++sequence), "content-type": "application/json" }, JSON.stringify(value)));
        },
        error(code: string, privateError = false) {
            const socket = sockets.at(-1)!;
            const destination = "/user/queue/messaging-errors";
            socket.ws.send(frame(privateError ? "MESSAGE" : "ERROR", privateError
                ? { destination, subscription: socket.subscriptions.get(destination)!, "message-id": String(++sequence) } : { message: code }, JSON.stringify({ code, message: "Test error" })));
        },
    };
}

test("matching receipt starts history; initial live overlap and REST echoes render once", async ({ page }, info) => {
    const broker = await setup(page, { manualReceipt: true });
    const errors: string[] = [];
    page.on("pageerror", error => errors.push(error.message));
    page.on("console", value => { if (value.type() === "error") errors.push(value.text()); });
    let historyCalls = 0;
    let release: () => void = () => {};
    const gate = new Promise<void>(resolve => { release = resolve; });
    await page.route("**/api/v1/conversations/*/messages*", async route => {
        if (route.request().method() === "POST") {
            broker.deliver(message(4, "REST and echo"));
            return route.fulfill({ json: message(4, "REST and echo") });
        }
        historyCalls++; await gate;
        return route.fulfill({ json: historyPage([message(1)]) });
    });
    await page.goto(`/conversations/${conversationId}`); await broker.subscribed();
    expect(historyCalls).toBe(0);
    broker.receipt("unrelated-send-receipt"); await flushPage(page); expect(historyCalls).toBe(0);
    broker.receipt(); await expect.poll(() => historyCalls).toBe(1);
    broker.deliver(message(3)); broker.deliver(message(2)); broker.deliver(message(1));
    broker.deliver({ ...message(9), conversationId: "other", content: "Forbidden cross-conversation" });
    release();
    const list = page.getByRole("list", { name: "Messages", exact: true });
    await expect(list.getByRole("listitem")).toHaveCount(3);
    await expect(list.locator("article > p:nth-child(2)")).toHaveText(["Message 1", "Message 2", "Message 3"]);
    await page.getByRole("textbox", { name: "Your message" }).fill("REST and echo");
    await page.getByRole("button", { name: "Send", exact: true }).click();
    await expect(list.getByRole("listitem")).toHaveCount(4);
    await expect(page.getByRole("textbox")).toHaveValue("");
    await expect(page.getByTestId("connection-status")).toHaveText("Live delivery connected.");
    await expect(page.getByText("Forbidden cross-conversation")).toHaveCount(0);
    await expect(page).toHaveTitle(/Kwatera/i);
    await expect(page).toHaveURL(`/conversations/${conversationId}`);
    await expect(page.locator("vite-error-overlay")).toHaveCount(0);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    expect(broker.commands.find(c => c.command === "CONNECT")?.headers.Authorization).toBe(`Bearer ${broker.jwt}`);
    expect(broker.commands.filter(c => c.command === "SUBSCRIBE").map(c => c.headers.destination)).toEqual(["/user/queue/messaging-errors", `/user/queue/conversations/${conversationId}/messages`]);
    expect(broker.commands.some(c => c.command === "SEND" || c.url.includes(broker.jwt))).toBe(false);
    expect(errors).toEqual([]);
    await page.evaluate(() => window.scrollTo(0, 0));
    if (process.env.KWATERA_SCREENSHOT_DIR) await page.screenshot({ path: join(process.env.KWATERA_SCREENSHOT_DIR, `live-${info.project.name}.png`), fullPage: false });
});

test("guest and owner each send through REST and both receive a single persisted copy", async ({ page, browser }) => {
    const ownerPage = await browser.newPage({ viewport: page.viewportSize() ?? undefined });
    try {
        const guestBroker = await setup(page);
        const ownerBroker = await setup(ownerPage, { userId: "owner" });
        const stored: ReturnType<typeof message>[] = [];
        for (const [view, userId] of [[page, "guest"], [ownerPage, "owner"]] as const) {
            await view.route("**/api/v1/conversations/*/messages*", async route => {
                if (route.request().method() !== "POST") return route.fulfill({ json: historyPage(stored) });
                const persisted = { ...message(stored.length + 1, route.request().postDataJSON().content), senderUserId: userId };
                stored.push(persisted);
                guestBroker.deliver(persisted); ownerBroker.deliver(persisted);
                return route.fulfill({ json: persisted });
            });
            await view.goto(`/conversations/${conversationId}`);
            await expect(view.getByRole("textbox")).toBeVisible();
        }
        for (const [view, content] of [[page, "Hello from guest"], [ownerPage, "Hello from owner"]] as const) {
            await view.getByRole("textbox").fill(content);
            await view.getByRole("button", { name: "Send", exact: true }).click();
            for (const participant of [page, ownerPage]) await expect(participant.getByText(content, { exact: true })).toHaveCount(1);
        }
        expect(stored).toHaveLength(2);
        expect(stored.map(m => m.senderUserId)).toEqual(["guest", "owner"]);
        await expect(page.getByRole("list", { name: "Messages", exact: true }).getByRole("listitem")).toHaveCount(2);
        await expect(ownerPage.getByRole("list", { name: "Messages", exact: true }).getByRole("listitem")).toHaveCount(2);
    } finally { await ownerPage.close(); }
});

test("transport reconnect recovers multiple opaque-cursor pages and concurrent live delivery", async ({ page }) => {
    const broker = await setup(page);
    let calls = 0, release: () => void = () => {};
    const gate = new Promise<void>(resolve => { release = resolve; });
    const cursors: (string | null)[] = [];
    await page.route("**/api/v1/conversations/*/messages*", async route => {
        calls++;
        if (calls === 1) return route.fulfill({ json: historyPage([message(1)]) });
        const cursor = new URL(route.request().url()).searchParams.get("cursor"); cursors.push(cursor);
        if (!cursor) { await gate; return route.fulfill({ json: historyPage([message(5), message(4)], "opaque +/==") }); }
        if (cursor === "opaque +/==") return route.fulfill({ json: historyPage([message(3), message(2)], "last==") });
        return route.fulfill({ json: historyPage([message(1)]) });
    });
    await page.goto(`/conversations/${conversationId}`); await broker.subscribed();
    await expect(page.getByText("Message 1", { exact: true })).toBeVisible();
    broker.sockets[0].ws.close({ code: 1011, reason: "Temporary transport failure" });
    await expect(page.getByTestId("connection-status")).toContainText(/disconnected|Reconnecting/);
    await broker.subscribed(2); await expect.poll(() => calls).toBe(2);
    broker.deliver(message(6)); broker.deliver(message(3)); release();
    const list = page.getByRole("list", { name: "Messages", exact: true });
    await expect(list.getByRole("listitem")).toHaveCount(6);
    expect(cursors).toEqual([null, "opaque +/==", "last=="]);
    await expect(list.locator("article > p:nth-child(2)")).toHaveText(Array.from({ length: 6 }, (_, i) => `Message ${i + 1}`));
    expect(broker.sockets[0].receipt).not.toBe(broker.sockets[1].receipt);
});

test("REST sending and manual refresh work throughout a transport outage", async ({ page }) => {
    const broker = await setup(page, { rejectTransport: true }); let posts = 0;
    await page.route("**/api/v1/conversations/*/messages*", async route => {
        if (route.request().method() === "POST") { posts++; return route.fulfill({ json: message(2, "Offline live transport") }); }
        return route.fulfill({ json: historyPage([message(1)]) });
    });
    await page.goto(`/conversations/${conversationId}`);
    await expect(page.getByText("Message 1", { exact: true })).toBeVisible();
    await page.getByRole("textbox").fill("Offline live transport");
    await page.getByRole("button", { name: "Send", exact: true }).click();
    await expect(page.getByText("Offline live transport", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Refresh", exact: true }).click();
    await expect(page.getByRole("button", { name: "Refresh", exact: true })).toBeEnabled();
    expect(posts).toBe(1); expect(broker.commands.some(c => c.command === "SEND")).toBe(false);
});

for (const [code, privateError, expected] of [
    ["UNAUTHENTICATED", false, "Your session is unavailable or has expired"],
    ["CONVERSATION_NOT_FOUND", true, "do not have access"],
] as const) {
    test(`${code} clears protected data and stops automatic reconnect`, async ({ page }) => {
        const broker = await setup(page); await page.goto(`/conversations/${conversationId}`); await broker.subscribed();
        await expect(page.getByText("Message 1", { exact: true })).toBeVisible();
        broker.error(code, privateError);
        await expect(page.getByRole("alert")).toContainText(expected);
        await expect(page.getByText("Live test stay")).toHaveCount(0);
        await expect(page.getByText("Message 1", { exact: true })).toHaveCount(0);
        await expect.poll(() => broker.sockets[0].closed).toBe(true);
        await page.clock.install(); await page.clock.fastForward(60_000);
        expect(broker.sockets.length).toBe(1);
    });
}

test("rejected subscription is actionable; private validation errors preserve the composer", async ({ page }) => {
    const broker = await setup(page); await page.goto(`/conversations/${conversationId}`); await broker.subscribed();
    await expect(page.getByRole("textbox")).toBeVisible();
    await page.getByRole("textbox").fill("Kept draft"); broker.error("INVALID_MESSAGE", true);
    await expect(page.getByRole("alert")).toContainText("The message was rejected");
    await expect(page.getByRole("textbox")).toHaveValue("Kept draft");
    broker.error("INVALID_DESTINATION");
    await expect(page.getByTestId("connection-status")).toContainText("Live subscription was rejected");
    await expect(page.getByTestId("connection-status").getByRole("button", { name: "Try again" })).toBeVisible();
    await expect(page.getByRole("textbox")).toHaveValue("Kept draft");
});

test("switching conversations unsubscribes; account changes discard pending history and metadata", async ({ page }) => {
    const broker = await setup(page); await page.goto(`/conversations/${conversationId}`); await broker.subscribed();
    await expect(page.getByRole("textbox")).toBeVisible();
    await page.getByRole("link", { name: "Back to messages" }).click();
    await expect.poll(() => broker.sockets[0].closed).toBe(true);
    expect(broker.commands.filter(c => c.command === "UNSUBSCRIBE")).toHaveLength(2);
    const secondId = "22222222-2222-2222-2222-222222222222";
    await page.evaluate(id => { history.pushState({}, "", `/conversations/${id}`); window.dispatchEvent(new PopStateEvent("popstate")); }, secondId);
    await broker.subscribed(2);
    await expect(page.getByRole("textbox")).toBeVisible();
    let release: () => void = () => {};
    const gate = new Promise<void>(resolve => { release = resolve; });
    let pending = false;
    await page.route("**/api/v1/conversations/*/messages*", async route => {
        pending = true; await gate;
        await route.fulfill({ json: historyPage([message(9, "Previous account secret")]) }).catch(() => {});
    });
    await page.getByRole("button", { name: "Refresh", exact: true }).click(); await expect.poll(() => pending).toBe(true);
    await page.route("**/api/v1/conversations/*", route => route.fulfill({ status: 404, json: {} }));
    await page.evaluate(jwt => { localStorage.setItem("token", jwt); window.dispatchEvent(new Event("kwatera-auth")); }, token("other"));
    await expect(page.getByRole("alert")).toContainText("do not have access");
    release();
    await expect(page.getByText("Previous account secret")).toHaveCount(0);
    await expect(page.getByText("Live test stay")).toHaveCount(0);
    await expect.poll(() => broker.sockets[1].closed).toBe(true);
    await page.evaluate(() => { localStorage.removeItem("token"); window.dispatchEvent(new Event("kwatera-auth")); });
    await expect(page).toHaveURL("/login");
});

test("live updates follow the bottom, preserve reading position and older-history scrolling", async ({ page }) => {
    const broker = await setup(page, { language: "pl" });
    await page.route("**/api/v1/conversations/*/messages*", (route: Route) => route.fulfill({ json: new URL(route.request().url()).searchParams.has("cursor")
        ? historyPage(Array.from({ length: 10 }, (_, i) => message(i)))
        : historyPage(Array.from({ length: 30 }, (_, i) => message(39 - i)), "older") }));
    await page.goto(`/conversations/${conversationId}`); await broker.subscribed();
    await expect(page.getByTestId("connection-status")).toHaveText("Odbiór wiadomości na żywo połączony.");
    const region = page.locator('div[role="region"][aria-label="Wiadomości"]');
    broker.deliver(message(40));
    await expect(page.getByText("Message 40", { exact: true })).toBeVisible();
    await expect.poll(() => region.evaluate(el => el.scrollHeight - el.scrollTop - el.clientHeight)).toBeLessThan(48);
    await region.evaluate(el => { el.scrollTop = 200; el.dispatchEvent(new Event("scroll")); });
    const top = await region.evaluate(el => el.scrollTop);
    broker.deliver(message(41)); await expect(page.getByText("Message 41", { exact: true })).toHaveCount(1);
    expect(await region.evaluate(el => el.scrollTop)).toBe(top);
    await region.evaluate(el => { el.scrollTop = 0; el.dispatchEvent(new Event("scroll")); });
    const height = await region.evaluate(el => el.scrollHeight);
    await page.getByRole("button", { name: "Wczytaj starsze wiadomości" }).click();
    await expect(page.getByText("Message 0", { exact: true })).toHaveCount(1);
    expect(await region.evaluate((el, oldHeight) => Math.abs(el.scrollTop - (el.scrollHeight - oldHeight)), height)).toBeLessThan(2);
});
