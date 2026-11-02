import { defineConfig } from "@playwright/test";
import { tmpdir } from "node:os";
import { join } from "node:path";

export default defineConfig({
    testDir: "./tests/e2e",
    outputDir: join(tmpdir(), "kwatera-conversation-tests"),
    fullyParallel: true,
    workers: 2,
    reporter: "list",
    use: {
        baseURL: "http://127.0.0.1:4173",
        browserName: "chromium",
        launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
            ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH } : {},
    },
    projects: [
        { name: "desktop", use: { viewport: { width: 1440, height: 1000 } } },
        { name: "mobile", use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true } },
    ],
    webServer: {
        command: "npm run preview -- --host 127.0.0.1 --port 4173 --strictPort",
        url: "http://127.0.0.1:4173",
        reuseExistingServer: !process.env.CI,
    },
});
