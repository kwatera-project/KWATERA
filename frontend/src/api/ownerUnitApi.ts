import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getPropertyUnits(
    propertyId: string
) {

    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/properties/${propertyId}/units`,
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