import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoProperties } from "../demo/demoProperties";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getMyProperties() {
    if (IS_DEMO_MODE) return demoProperties.slice(0, 2);

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
