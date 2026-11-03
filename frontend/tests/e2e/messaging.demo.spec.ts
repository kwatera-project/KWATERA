import { test, expect } from "@playwright/test";

for (const role of ["ROLE_GUEST", "ROLE_OWNER", "ROLE_ADMIN"]) {
    test(`static demo hides messaging for ${role}`, async ({ page }) => {
        const token = `demo.${Buffer.from(JSON.stringify({ role, userId: "demo-user-guest", firstName: "Demo" })).toString("base64url")}.signature`;
        await page.addInitScript(token => {
            localStorage.setItem("token", token);
            localStorage.setItem("language", "en");
        }, token);
        let messagingRequests = 0;
        page.on("request", request => {
            if (/\/api\/v1\/(conversations|reservations\/[^/]+\/conversation)/.test(request.url())) messagingRequests++;
        });
        await page.goto("/about");
        await page.getByRole("button", { name: "My Account", exact: true }).click();
        await expect(page.getByRole("link", { name: "Messages", exact: true })).toHaveCount(0);
        const reservationsPath = role === "ROLE_GUEST" ? "/my-reservations" : "/admin/reservations";
        await page.goto(reservationsPath);
        await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
        await expect(page.getByRole("link", { name: /^view details$/i }).first()).toBeVisible();
        await expect(page.getByRole("button", { name: "Open conversation", exact: true })).toHaveCount(0);
        await page.getByRole("link", { name: /^view details$/i }).first().click();
        await expect(page).toHaveURL(/\/reservations\/demo-reservation/);
        await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
        await expect(page.getByRole("button", { name: "Open conversation", exact: true })).toHaveCount(0);
        expect(messagingRequests).toBe(0);
    });
}
