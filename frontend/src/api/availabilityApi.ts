import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoOccupiedDatesByUnit } from "../demo/demoProperties";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function checkAvailability(unitId: string, from: string, to: string) {
    if (IS_DEMO_MODE) {
        const overlaps = (demoOccupiedDatesByUnit[unitId] ?? []).some((range) => from < range.endDate && to > range.startDate);
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
