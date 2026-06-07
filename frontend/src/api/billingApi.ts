import { GATEWAY_BASE_URL } from "./apiConfig";

export interface CheckoutPayload {
    type: "ACCOMMODATION";
    description: string;
    quantity: number;
    unitPrice: number;
}

export async function createCheckoutSession(reservationId: string, payload: CheckoutPayload): Promise<string> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to proceed with payment");
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/billing/checkout/${reservationId}`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        throw new Error(`Checkout failed: ${res.status}`);
    }

    const checkoutUrl = await res.text();

    try {
        new URL(checkoutUrl);
    } catch {
        throw new Error("Invalid checkout URL received");
    }

    return checkoutUrl;
}
