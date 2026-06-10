import { GATEWAY_BASE_URL } from "./apiConfig";

export type SystemEventType =
    | "RESERVATION_CREATED"
    | "MANUAL_RESERVATION_CREATED"
    | "UNIT_BLOCKED"
    | "RESERVATION_STATUS_CHANGED"
    | "EXPIRED_RESERVATION_CANCELLED";

export interface SystemEvent {
    id: string;
    timestamp: string;
    actionType: SystemEventType;
    actorUserId: string | null;
    entityType: string | null;
    entityId: string | null;
    details: string | null;
}

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

export async function getSystemEvents(actionType?: SystemEventType | "ALL", limit = 100): Promise<SystemEvent[]> {
    const token = localStorage.getItem("token");
    const params = new URLSearchParams();
    params.append("limit", String(limit));
    if (actionType && actionType !== "ALL") {
        params.append("actionType", actionType);
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/v1/admin/system-events?${params.toString()}`, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    if (!res.ok) throw new Error("Failed to fetch system events");
    return res.json();
}
