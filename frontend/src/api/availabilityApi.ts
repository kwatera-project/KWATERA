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