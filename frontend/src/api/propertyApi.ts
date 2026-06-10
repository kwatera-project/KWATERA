import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function getProperties(
    minLat?: number,
    maxLat?: number,
    minLng?: number,
    maxLng?: number,
    amenities?: string[]
) {
    let url = `${API_URL}/properties`;
    const params = new URLSearchParams();
    if (minLat !== undefined && maxLat !== undefined && minLng !== undefined && maxLng !== undefined) {
        params.set("minLat", String(minLat));
        params.set("maxLat", String(maxLat));
        params.set("minLng", String(minLng));
        params.set("maxLng", String(maxLng));
    }
    if (amenities && amenities.length > 0) {
        amenities.forEach(a => params.append("amenities", a));
    }
    const queryString = params.toString();
    if (queryString) {
        url += `?${queryString}`;
    }
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

export async function getProperty(id: string) {
    const res = await fetch(`${API_URL}/properties/${id}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
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

export async function getUnitImages(propertyId: string, unitId: string) {
    const res = await fetch(`${API_URL}/properties/${propertyId}/units/${unitId}/images`);
    return res.json();
}