import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function getProperties(minLat?: number, maxLat?: number, minLng?: number, maxLng?: number) {
    let url = `${API_URL}/properties`;
    if (minLat !== undefined && maxLat !== undefined && minLng !== undefined && maxLng !== undefined) {
        url += `?minLat=${minLat}&maxLat=${maxLat}&minLng=${minLng}&maxLng=${maxLng}`;
    }
    const res = await fetch(url);
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

export async function getUnitImages(propertyId: string, unitId: string) {
    const res = await fetch(`${API_URL}/properties/${propertyId}/units/${unitId}/images`);
    return res.json();
}