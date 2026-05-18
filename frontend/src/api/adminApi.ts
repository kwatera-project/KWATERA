import { GATEWAY_BASE_URL } from "./apiConfig";

export async function getOccupancy(startDate: string, endDate: string) {
    const token = localStorage.getItem("token");
    const res = await fetch(
        `${GATEWAY_BASE_URL}/api/v1/admin/occupancy?startDate=${startDate}&endDate=${endDate}`,
        {
            headers: {
                Authorization: `Bearer ${token}`
            }
        }
    );
    if (!res.ok) throw new Error("Failed to fetch occupancy");
    return res.json();
}