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
    maxLng?: number,
    amenities?: string[]
) {
    const hasBounds =
        minLat !== undefined &&
        maxLat !== undefined &&
        minLng !== undefined &&
        maxLng !== undefined;

    if (IS_DEMO_MODE) {
        let filteredProperties = demoProperties;

        if (hasBounds) {
            filteredProperties = filteredProperties.filter((property) =>
                typeof property.latitude === "number" &&
                typeof property.longitude === "number" &&
                property.latitude >= minLat! &&
                property.latitude <= maxLat! &&
                property.longitude >= minLng! &&
                property.longitude <= maxLng!
            );
        }

        if (amenities && amenities.length > 0) {
            filteredProperties = filteredProperties.filter((property) =>
                amenities.every((amenity) => (property.amenities ?? []).includes(amenity))
            );
        }

        return filteredProperties;
    }

    let url = `${API_URL}/properties`;
    const params = new URLSearchParams();

    if (hasBounds) {
        params.set("minLat", String(minLat));
        params.set("maxLat", String(maxLat));
        params.set("minLng", String(minLng));
        params.set("maxLng", String(maxLng));
    }

    if (amenities && amenities.length > 0) {
        amenities.forEach((amenity) => params.append("amenities", amenity));
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
    if (IS_DEMO_MODE) {
        return demoProperties.find((property) => property.id === id) ?? null;
    }

    const res = await fetch(`${API_URL}/properties/${id}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
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