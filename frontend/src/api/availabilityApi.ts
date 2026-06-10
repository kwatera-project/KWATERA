import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function checkAvailability(unitId: string, from: string, to: string) {
    const res = await fetch(
        `${API_URL}/availability?unitId=${unitId}&from=${from}&to=${to}`
    );
    return res.json();
}

export async function getOccupiedDates(unitId: string) {
    const res = await fetch(`${API_URL}/availability/occupied-dates?unitId=${unitId}`);
    return res.json();
}

export async function createBlock(
    unitId: string,
    from: string,
    to: string,
    reason: string
) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to block dates");
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/v1/reservations`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            unitId: unitId,
            startDate: from,
            endDate: to,
            currency: "PLN",
            status: "BLOCKED",
            reason: reason // send reason directly or inside body
        })
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to block dates");
    }

    return res.json();
}