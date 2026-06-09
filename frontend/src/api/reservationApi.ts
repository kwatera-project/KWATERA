import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoAdminReservations, demoGuestReservations, demoReservations } from "../demo/demoReservations";

const API_URL = `${GATEWAY_BASE_URL}/api/v1/reservations`;
let demoCreatedReservationCounter = 1;

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

    if (IS_DEMO_MODE) {
        const nights = Math.max(1, Math.ceil((new Date(to).getTime() - new Date(from).getTime()) / (1000 * 60 * 60 * 24)));
        const totalPrice = Number((nights * 420).toFixed(2));
        return {
            id: `demo-created-reservation-${demoCreatedReservationCounter++}`,
            unitId,
            startDate: from,
            endDate: to,
            status: "CONFIRMED",
            totalPrice,
            convertedTotalPrice: totalPrice,
            currencyInfo: { baseCurrency: "PLN", displayCurrency: currency, exchangeRate: 1, rateEffectiveDate: "2026-06-09" },
            ...extraDetails,
        };
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

export async function getReservationDetails(id: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view this reservation");
    }

    if (IS_DEMO_MODE) {
        const details = demoReservations.find((reservation) => reservation.id === id)
            ?? demoAdminReservations.find((reservation) => reservation.id === id);
        if (!details) throw new Error("Demo reservation not found");
        return details;
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

    if (IS_DEMO_MODE) return demoGuestReservations;

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
