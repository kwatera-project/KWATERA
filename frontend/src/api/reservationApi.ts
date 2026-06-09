import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api/v1/reservations`;

export async function createReservation(
    unitId: string,
    from: string,
    to: string,
    currency: string,
    extraDetails?: Record<string, unknown>
) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to book this unit");
    }

    const res = await fetch(API_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            unitId: unitId,
            startDate: from,
            endDate: to,
            currency: currency,
            ...extraDetails
        })
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to create reservation");
    }

    return res.json();
}

export async function createManualReservation(
    unitId: string,
    from: string,
    to: string,
    guestEmail: string,
    guestDetails: {
        firstName: string;
        lastName: string;
        phone: string;
        note?: string;
    }
) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to create a manual reservation");
    }

    const res = await fetch(API_URL, {
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
            guestEmail: guestEmail,
            status: "CONFIRMED",
            ...guestDetails
        })
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to create manual reservation");
    }

    return res.json();
}

export async function getReservationDetails(id: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view this reservation");
    }

    const res = await fetch(`${API_URL}/${id}`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch reservation details");
    }

    return res.json();
}

export async function getMyReservations() {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view your reservations");
    }

    const res = await fetch(`${API_URL}/my`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch your reservations");
    }

    return res.json();
}