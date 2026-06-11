import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoOccupiedDatesByUnit, demoUnitsByProperty } from "../demo/demoProperties";
import { demoOccupancy } from "../demo/demoReservations";

const API_URL = `${GATEWAY_BASE_URL}/api`;

let demoCreatedBlockCounter = 1;

export async function checkAvailability(unitId: string, from: string, to: string) {
    if (IS_DEMO_MODE) {
        const overlaps = (demoOccupiedDatesByUnit[unitId] ?? []).some(
            (range) => from < range.endDate && to > range.startDate
        );

        return {
            available: !overlaps,
            message: overlaps ? "Selected dates overlap with a demo reservation." : "Available in demo mode.",
        };
    }

    const res = await fetch(
        `${API_URL}/availability?unitId=${unitId}&from=${from}&to=${to}`
    );
    return res.json();
}

export async function getOccupiedDates(unitId: string) {
    if (IS_DEMO_MODE) return demoOccupiedDatesByUnit[unitId] ?? [];

    const res = await fetch(`${API_URL}/availability/occupied-dates?unitId=${unitId}`);
    return res.json();
}

export async function createBlock(
    unitId: string,
    from: string,
    to: string,
    reason: string
) {
    if (IS_DEMO_MODE) {
        const blockId = `demo-block-${demoCreatedBlockCounter++}`;

        if (!demoOccupiedDatesByUnit[unitId]) {
            demoOccupiedDatesByUnit[unitId] = [];
        }

        demoOccupiedDatesByUnit[unitId].push({
            startDate: from,
            endDate: to,
        });

        const unitName =
            Object.values(demoUnitsByProperty)
                .flat()
                .find((unit) => unit.id === unitId)?.name ?? unitId;

        demoOccupancy.push({
            reservationId: blockId,
            unitId,
            unitName,
            startDate: from,
            endDate: to,
            status: "BLOCKED",
            guestName: reason || "Maintenance block",
            totalPrice: 0,
        });

        return {
            id: blockId,
            unitId,
            startDate: from,
            endDate: to,
            currency: "PLN",
            status: "BLOCKED",
            reason,
        };
    }

    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to block dates");
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/v1/reservations`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`,
        },
        body: JSON.stringify({
            unitId,
            startDate: from,
            endDate: to,
            currency: "PLN",
            status: "BLOCKED",
            reason,
        }),
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to block dates");
    }

    return res.json();
}