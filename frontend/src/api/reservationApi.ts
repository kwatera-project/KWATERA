import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoAdminReservations, demoGuestReservations, demoOccupancy, demoReservations } from "../demo/demoReservations";
import { demoOccupiedDatesByUnit, demoProperties, demoUnitsByProperty } from "../demo/demoProperties";
import type { ReservationDetails } from "../types/reservation";

const API_URL = `${GATEWAY_BASE_URL}/api/v1/reservations`;
let demoCreatedReservationCounter = 1;

function findDemoUnit(unitId: string) {
    for (const [propertyId, units] of Object.entries(demoUnitsByProperty)) {
        const unit = units.find((item) => item.id === unitId);
        if (unit) {
            return {
                unit,
                property: demoProperties.find((item) => item.id === propertyId),
            };
        }
    }

    return null;
}

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
    if (IS_DEMO_MODE) {
        const demoUnit = findDemoUnit(unitId);
        if (!demoUnit) throw new Error("Demo unit not found");

        const nights = Math.max(1, Math.ceil((new Date(to).getTime() - new Date(from).getTime()) / (1000 * 60 * 60 * 24)));
        const pricePerNightSnapshot = demoUnit.unit.pricePerNight;
        const totalPrice = Number((nights * pricePerNightSnapshot).toFixed(2));
        const guestName = `${guestDetails.firstName} ${guestDetails.lastName}`.trim();
        const reservationId = `demo-manual-reservation-${demoCreatedReservationCounter++}`;

        const reservation: ReservationDetails = {
            id: reservationId,
            unitId,
            guestName,
            guestEmail,
            unitName: demoUnit.unit.name,
            city: demoUnit.property?.city,
            startDate: from,
            endDate: to,
            status: "CONFIRMED",
            userId: "demo-manual-guest",
            createdAt: new Date().toISOString(),
            pricePerNightSnapshot,
            totalPrice,
            convertedTotalPrice: totalPrice,
            currencyInfo: {
                baseCurrency: "PLN",
                displayCurrency: "PLN",
                exchangeRate: 1,
                rateEffectiveDate: "2026-06-09",
            },
            ownerName: "Marcus Green",
            ownerEmail: "owner.demo@kwatera.local",
            guestMessage: guestDetails.note,
        };

        demoReservations.push(reservation);
        demoAdminReservations.push({
            id: reservation.id,
            guestName: reservation.guestName,
            unitName: reservation.unitName,
            startDate: reservation.startDate,
            endDate: reservation.endDate,
            status: reservation.status,
            userId: reservation.userId,
            pricePerNightSnapshot: reservation.pricePerNightSnapshot,
            totalPrice: reservation.totalPrice,
        });
        demoOccupancy.push({
            reservationId: reservation.id,
            unitId: reservation.unitId,
            unitName: reservation.unitName,
            startDate: reservation.startDate,
            endDate: reservation.endDate,
            status: reservation.status,
            guestName: reservation.guestName,
            totalPrice: reservation.totalPrice,
            guestEmail: reservation.guestEmail,
        });

        if (!demoOccupiedDatesByUnit[unitId]) {
            demoOccupiedDatesByUnit[unitId] = [];
        }

        demoOccupiedDatesByUnit[unitId].push({
            startDate: from,
            endDate: to,
        });

        return reservation;
    }

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
