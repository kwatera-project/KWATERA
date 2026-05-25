import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api/billing/settlements`;

export async function getSettlementDetails(id: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view this settlement");
    }

    const res = await fetch(`${API_URL}/${id}`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch settlement details");
    }

    const data = await res.json();
    return data.settlement;
}

export async function getSettlementItemInfoByType(id: string, type: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view this settlement");
    }

    const res = await fetch(`${API_URL}/${id}/${type}`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch settlement details");
    }

    return res.json();
}