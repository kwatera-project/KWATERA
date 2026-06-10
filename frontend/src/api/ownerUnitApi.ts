import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { getDemoUnits } from "../demo/demoProperties";
import type { UnitCreateRequest, UnitUpdateRequest } from "../types/property.ts";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getPropertyUnits(
    propertyId: string
) {
    if (IS_DEMO_MODE) return getDemoUnits(propertyId);

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
    if (IS_DEMO_MODE) {
        return {
            id: `demo-created-unit-${Date.now()}`,
            propertyId,
            ...data,
        };
    }

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
    if (IS_DEMO_MODE) {
        void currency;
        return {
            id: unitId,
            propertyId,
            ...data,
        };
    }

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
    if (IS_DEMO_MODE) {
        void propertyId;
        void unitId;
        return;
    }

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
            message: errorMessage,
        };
    }
}

export async function uploadUnitImage(
    propertyId: string,
    unitId: string,
    file: File,
    isMain: boolean
) {
    if (IS_DEMO_MODE) {
        void propertyId;
        void unitId;
        void file;
        void isMain;
        return;
    }

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
    if (IS_DEMO_MODE) {
        void propertyId;
        void unitId;
        void imageId;
        return;
    }

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
    if (IS_DEMO_MODE) {
        void propertyId;
        void unitId;
        void imageId;
        void isMain;
        return;
    }

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