import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api/predict`;

export async function getPredictedPrice(
    propertyId: string,
    unitId: string,
    date?: string
) {
    const token = localStorage.getItem("token");

    let finalUrl = `${API_URL}/price/property/${propertyId}/unit/${unitId}`;
    if (date) {
        finalUrl += `/date/${date}`;
    }

    const res = await fetch(finalUrl, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`,
        },
    });

    if (!res.ok) {
        let errorMessage = "Failed to fetch predicted price";

        try {
            const errorData = await res.json();
            errorMessage = errorData.message || errorMessage;
        } catch {}

        throw new Error(errorMessage);
    }

    return res.json() as Promise<number>;
}