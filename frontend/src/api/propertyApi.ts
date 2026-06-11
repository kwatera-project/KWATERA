import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import {
    demoProperties,
    demoPropertyImages,
    demoUnitsByProperty,
    getDemoUnits,
} from "../demo/demoProperties";

const API_URL = `${GATEWAY_BASE_URL}/api`;

export async function getProperties(
    minLat?: number,
    maxLat?: number,
    minLng?: number,
    maxLng?: number
) {
    if (IS_DEMO_MODE) {
        if (
            minLat !== undefined &&
            maxLat !== undefined &&
            minLng !== undefined &&
            maxLng !== undefined
        ) {
            return demoProperties.filter((property) =>
                typeof property.latitude === "number" &&
                typeof property.longitude === "number" &&
                property.latitude >= minLat &&
                property.latitude <= maxLat &&
                property.longitude >= minLng &&
                property.longitude <= maxLng
            );
        }

        return demoProperties;
    }

    let url = `${API_URL}/properties`;

    if (
        minLat !== undefined &&
        maxLat !== undefined &&
        minLng !== undefined &&
        maxLng !== undefined
    ) {
        url += `?minLat=${minLat}&maxLat=${maxLat}&minLng=${minLng}&maxLng=${maxLng}`;
    }

    const res = await fetch(url);
    return res.json();
}

export async function getProperty(id: string) {
    if (IS_DEMO_MODE) {
        return demoProperties.find((property) => property.id === id) ?? null;
    }

    const res = await fetch(`${API_URL}/properties/${id}`);
    return res.json();
}

export async function getUnit(unitId: string, currency: string = "PLN") {
    if (IS_DEMO_MODE) {
        void currency;

        return Object.values(demoUnitsByProperty)
            .flat()
            .find((unit) => unit.id === unitId) ?? null;
    }

    const res = await fetch(`${API_URL}/properties/units/${unitId}?currency=${currency}`);
    return res.json();
}

export async function getUnits(propertyId: string, currency: string = "PLN") {
    if (IS_DEMO_MODE) return getDemoUnits(propertyId, currency);

    const res = await fetch(`${API_URL}/properties/${propertyId}/units?currency=${currency}`);
    return res.json();
}

export async function getPropertyImages(id: string) {
    if (IS_DEMO_MODE) {
        return (demoPropertyImages[id] ?? []).map((url, index) => ({
            id: `${id}-demo-image-${index + 1}`,
            url,
            isMain: index === 0,
        }));
    }

    const res = await fetch(`${API_URL}/properties/${id}/images`);
    return res.json();
}

export async function getUnitImages(propertyId: string, unitId: string) {
    if (IS_DEMO_MODE) {
        void propertyId;
        void unitId;
        return [];
    }

    const res = await fetch(`${API_URL}/properties/${propertyId}/units/${unitId}/images`);
    return res.json();
}