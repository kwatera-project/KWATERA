import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";

export async function subscribeToNewsletter(email: string): Promise<string> {
    if (IS_DEMO_MODE) {
        return "Thank you for subscribing!";
    }
    const res = await fetch(`${GATEWAY_BASE_URL}/api/newsletter/subscribe`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ email })
    });
    if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || "Failed to subscribe to newsletter");
    }
    return res.text();
}
