import { GATEWAY_BASE_URL } from "./apiConfig";
import type {PropertyCreateRequest, PropertyUpdateRequest} from "../types/property.ts";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getMyProperties() {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/properties`,
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res.json();
}

export async function createProperty(data: PropertyCreateRequest) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res.json();
}

export async function updateProperty(
    propertyId: string,
    data: PropertyUpdateRequest
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}`,
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(data),
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res.json();
}

export async function deleteProperty(
    propertyId: string
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}`,
        {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
}