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

export async function getDashboardReservationMetrics(startDate?: string, endDate?: string) {
    const token = localStorage.getItem("token");
    let url = `${GATEWAY_BASE_URL}/api/v1/admin/dashboard/reservations`;
    const params = new URLSearchParams();
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);
    const queryString = params.toString();
    if (queryString) {
        url += `?${queryString}`;
    }

    const res = await fetch(url, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    if (!res.ok) throw new Error("Failed to fetch reservation dashboard metrics");
    return res.json();
}

export async function getDashboardBillingMetrics(startDate?: string, endDate?: string) {
    const token = localStorage.getItem("token");
    let url = `${GATEWAY_BASE_URL}/api/v1/admin/dashboard/billing`;
    const params = new URLSearchParams();
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);
    const queryString = params.toString();
    if (queryString) {
        url += `?${queryString}`;
    }

    const res = await fetch(url, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    if (!res.ok) throw new Error("Failed to fetch billing dashboard metrics");
    return res.json();
}