export type DemoRole = "guest" | "owner" | "admin";

export const DEMO_TOKEN_STORAGE_KEY = "token";
export const DEMO_ROLE_STORAGE_KEY = "kwatera-demo-role";

export function isDemoToken(token: string | null) {
    return !!token && token.startsWith("demo.");
}

function base64UrlEncode(value: unknown) {
    const json = JSON.stringify(value);
    const base64 = btoa(unescape(encodeURIComponent(json)));
    return base64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function createDemoToken(payload: Record<string, unknown>) {
    const body = base64UrlEncode({
        iss: "kwatera-demo",
        aud: "kwatera-poster-demo",
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + 60 * 60 * 24,
        ...payload,
    });

    return `demo.${body}.signature`;
}
