import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function getProperties() {
    const res = await fetch(`${API_URL}/properties`);
    return res.json();
}

export async function getProperty(id: string) {
    const res = await fetch(`${API_URL}/properties/${id}`);
    return res.json();
}

export async function getUnit(unitId: string, currency: string = "PLN") {
    const res = await fetch(`${API_URL}/properties/units/${unitId}?currency=${currency}`);
    return res.json();
}

export async function getUnits(propertyId: string, currency: string = "PLN") {
    const res = await fetch(`${API_URL}/properties/${propertyId}/units?currency=${currency}`);
    return res.json();
}

export async function getPropertyImages(id: string) {
    const res = await fetch(`${API_URL}/properties/${id}/images`);
    return res.json();
}