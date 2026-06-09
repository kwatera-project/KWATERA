import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoProperties, demoPropertyImages, getDemoUnits } from "../demo/demoProperties";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function getProperties() {
    if (IS_DEMO_MODE) return demoProperties;

    const res = await fetch(`${API_URL}/properties`);
    return res.json();
}

export async function getProperty(id: string) {
    if (IS_DEMO_MODE) return demoProperties.find((property) => property.id === id) ?? null;

    const res = await fetch(`${API_URL}/properties/${id}`);
    return res.json();
}

export async function getUnits(propertyId: string, currency: string = "PLN") {
    if (IS_DEMO_MODE) return getDemoUnits(propertyId, currency);

    const res = await fetch(`${API_URL}/properties/${propertyId}/units?currency=${currency}`);
    return res.json();
}

export async function getPropertyImages(id: string) {
    if (IS_DEMO_MODE) return demoPropertyImages[id] ?? [];

    const res = await fetch(`${API_URL}/properties/${id}/images`);
    return res.json();
}
