import { defineConfig } from "@playwright/test";
import config from "./playwright.config";

export default defineConfig({
    ...config,
    testIgnore: [],
    testMatch: "**/*.demo.spec.ts",
    use: { ...config.use, baseURL: "http://127.0.0.1:4174" },
    webServer: {
        command: "npm run dev:demo -- --host 127.0.0.1 --port 4174 --strictPort",
        url: "http://127.0.0.1:4174",
        reuseExistingServer: !process.env.CI,
    },
});
