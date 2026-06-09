import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { getDemoUnits } from "../demo/demoProperties";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getPropertyUnits(
    propertyId: string
) {
    if (IS_DEMO_MODE) return getDemoUnits(propertyId);

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
