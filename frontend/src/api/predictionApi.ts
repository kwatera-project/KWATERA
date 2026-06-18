import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { getDemoUnits } from "../demo/demoProperties";

const API_URL = `${GATEWAY_BASE_URL}/api/predict`;

export async function getPredictedPrice(
    propertyId: string,
    unitId: string,
    date?: string
) {
    if (IS_DEMO_MODE) {
        void date;
        const unit = getDemoUnits(propertyId).find((item) => item.id === unitId);
        return Number(((unit?.pricePerNight ?? 420) * 1.08).toFixed(2));
    }

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
        } catch {
            // Default error massage
        }

        throw new Error(errorMessage);
    }

    return res.json() as Promise<number>;
}
