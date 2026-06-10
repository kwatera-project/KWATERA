import { GATEWAY_BASE_URL } from "./apiConfig";
import type {UnitCreateRequest, UnitUpdateRequest} from "../types/property.ts";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getPropertyUnits(
    propertyId: string
) {

    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/units`,
        {
            headers: {
                "Authorization": `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res.json();
}

export async function createUnit(
    propertyId: string,
    data: UnitCreateRequest
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/units`,
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

export async function updateUnit(
    propertyId: string,
    unitId: string,
    data: UnitUpdateRequest,
    currency = "PLN"
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/unit/${unitId}?currency=${currency}`,
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

export async function deleteUnit(
    propertyId: string,
    unitId: string
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/unit/${unitId}`,
        {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        let errorMessage = "Failed to delete unit";

        try {
            const errorData = await res.json();
            errorMessage = errorData.message || errorMessage;
        } catch {
            errorMessage = await res.text() || errorMessage;
        }

        throw {
            status: res.status,
            message: errorMessage
        };
    }
}

export async function uploadUnitImage(
    propertyId: string,
    unitId: string,
    file: File,
    isMain: boolean
) {
    const token = localStorage.getItem("token");

    const formData = new FormData();
    formData.append("file", file);
    formData.append("isMain", String(isMain));

    const res = await fetch(
        `${API_URL}/property/${propertyId}/unit/${unitId}/images`,
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
            },
            body: formData,
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
}

export async function deleteUnitImage(
    propertyId: string,
    unitId: string,
    imageId: string
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/unit/${unitId}/images/${imageId}`,
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

export async function setUnitImageAsMain(
    propertyId: string,
    unitId: string,
    imageId: string,
    isMain: boolean
) {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/unit/${unitId}/images/${imageId}/main?isMain=${isMain}`,
        {
            method: "PATCH",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
}