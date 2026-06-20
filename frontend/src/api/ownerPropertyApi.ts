import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoProperties } from "../demo/demoProperties";
import type { PropertyCreateRequest, PropertyUpdateRequest } from "../types/property.ts";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getMyProperties() {
    if (IS_DEMO_MODE) return demoProperties;

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
    if (IS_DEMO_MODE) {
        return {
            id: `demo-created-property-${Date.now()}`,
            ...data,
        };
    }

    const token = localStorage.getItem("token");
    const body = { ...data };

    const res = await fetch(
        `${API_URL}/property`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(body),
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
    if (IS_DEMO_MODE) {
        return {
            id: propertyId,
            ...data,
        };
    }

    const token = localStorage.getItem("token");
    const body = { ...data};

    const res = await fetch(
        `${API_URL}/property/${propertyId}`,
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(body),
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
    if (IS_DEMO_MODE) {
        void propertyId;
        return;
    }

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
        let errorMessage = "Failed to delete property";

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

export async function uploadPropertyImage(
    propertyId: string,
    file: File,
    isMain: boolean
) {
    if (IS_DEMO_MODE) {
        void propertyId;
        void file;
        void isMain;
        return;
    }

    const token = localStorage.getItem("token");

    const formData = new FormData();
    formData.append("file", file);
    formData.append("isMain", String(isMain));

    const res = await fetch(
        `${API_URL}/property/${propertyId}/images`,
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

export async function deletePropertyImage(
    propertyId: string,
    imageId: string
) {
    if (IS_DEMO_MODE) {
        void propertyId;
        void imageId;
        return;
    }

    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/property/${propertyId}/images/${imageId}`,
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

export async function setPropertyImageAsMain(
    propertyId: string,
    imageId: string,
    isMain: boolean
) {
    if (IS_DEMO_MODE) {
        void propertyId;
        void imageId;
        void isMain;
        return;
    }

    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/${propertyId}/images/${imageId}/main?isMain=${isMain}`,
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