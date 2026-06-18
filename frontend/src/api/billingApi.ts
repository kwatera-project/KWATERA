import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";

export interface CheckoutPayload {
    type: "ACCOMMODATION" | "DEPOSIT" | "ELECTRICITY" | "WATER" | "CLEANING_FEE";
    description: string;
    quantity: number;
    unitPrice: number;
}

export async function createCheckoutSession(reservationId: string, payload: CheckoutPayload): Promise<string> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to proceed with payment");
    }

    if (IS_DEMO_MODE) {
        return `${window.location.origin}${window.location.pathname}#/demo-payment-success?reservationId=${reservationId}&amount=${payload.unitPrice}`;
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
