import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";

export async function forgotPassword(email: string): Promise<void> {
    if (IS_DEMO_MODE) {
        return;
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/auth/forgot-password`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ email })
    });

    if (!res.ok) {
        throw new Error("Failed to request password reset");
    }
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
    if (IS_DEMO_MODE) {
        return;
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/auth/reset-password`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ token, newPassword })
    });

    if (!res.ok) {
        throw new Error("Failed to reset password");
    }
}
