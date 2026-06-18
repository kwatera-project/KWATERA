import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoOccupancy, demoAdminReservations } from "../demo/demoReservations";
import { demoBillingMetrics, demoReservationMetrics } from "../demo/demoReports";
import { demoSystemEvents } from "../demo/demoSystemEvents";

export type SystemEventType =
    | "RESERVATION_CREATED"
    | "MANUAL_RESERVATION_CREATED"
    | "UNIT_BLOCKED"
    | "RESERVATION_STATUS_CHANGED"
    | "EXPIRED_RESERVATION_CANCELLED"
    | "OCR_READING_ATTEMPTED"
    | "OCR_READING_SUCCEEDED"
    | "OCR_READING_FAILED"
    | "METER_READING_MANUALLY_CORRECTED"
    | "MEDIA_SETTLEMENT_GENERATED"
    | "PAYMENT_FAILED"
    | "PAYMENT_CANCELLED"
    | "BALANCE_CHANGED";

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
    if (IS_DEMO_MODE) {
        return demoOccupancy.filter((item) => item.startDate <= endDate && item.endDate >= startDate);
    }

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
    if (IS_DEMO_MODE) {
        void startDate;
        void endDate;
        return demoReservationMetrics;
    }

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
    if (IS_DEMO_MODE) {
        void startDate;
        void endDate;
        return demoBillingMetrics;
    }

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

export type SystemEventsQuery = {
    actionType?: SystemEventType | "ALL";
    from?: string | null;
    to?: string | null;
    limit?: number;
};

export async function getSystemEvents(query: SystemEventsQuery = {}): Promise<SystemEvent[]> {
    const token = localStorage.getItem("token");
    const params = new URLSearchParams();
    const { actionType, from, to, limit = 100 } = query;

    if (IS_DEMO_MODE) {
        const fromTime = from ? new Date(from).getTime() : null;
        const toTime = to ? new Date(to).getTime() : null;

        return demoSystemEvents
            .filter((event) => !actionType || actionType === "ALL" || event.actionType === actionType)
            .filter((event) => {
                const timestamp = new Date(event.timestamp).getTime();
                return (fromTime === null || timestamp >= fromTime) && (toTime === null || timestamp <= toTime);
            })
            .sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime())
            .slice(0, limit);
    }

    if (actionType && actionType !== "ALL") {
        params.append("actionType", actionType);
    }
    if (from) params.append("from", from);
    if (to) params.append("to", to);
    if (limit) params.append("limit", String(limit));

    const queryString = params.toString();
    const url = queryString
        ? `${GATEWAY_BASE_URL}/api/v1/admin/system-events?${queryString}`
        : `${GATEWAY_BASE_URL}/api/v1/admin/system-events`;

    const res = await fetch(url, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    if (!res.ok) throw new Error("Failed to fetch system events");
    return res.json();
}

export async function getAdminReservations(status?: string, startDate?: string, endDate?: string) {
    if (IS_DEMO_MODE) {
        let filtered = demoAdminReservations;
        if (status) {
            filtered = filtered.filter((reservation) => reservation.status === status);
        }
        if (startDate && endDate) {
            filtered = filtered.filter((reservation) => reservation.startDate <= endDate && reservation.endDate >= startDate);
        }
        return filtered;
    }

    const token = localStorage.getItem("token");
    const params = new URLSearchParams();
    if (status) params.append("status", status);
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);
    const queryString = params.toString();
    const url = queryString
        ? `${GATEWAY_BASE_URL}/api/v1/admin/reservations?${queryString}`
        : `${GATEWAY_BASE_URL}/api/v1/admin/reservations`;

    const res = await fetch(url, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
    });

    if (!res.ok) throw new Error("Get data error");
    return res.json();
}

export async function updateAdminReservationStatus(id: string, newStatus: string) {
    if (IS_DEMO_MODE) return { id, status: newStatus };

    const token = localStorage.getItem("token");
    const res = await fetch(`${GATEWAY_BASE_URL}/api/v1/admin/reservations/${id}/status`, {
        method: "PATCH",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ newStatus }),
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({ message: "An error occurred" }));
        const message = res.status === 400
            ? "This status transition is not allowed"
            : res.status === 401
              ? "Session expired or invalid. Please log in again"
              : res.status === 403
                ? "You are not allowed to update this reservation"
                : res.status === 404
                  ? "Reservation not found"
                  : errorData.message || "An error occurred";
        throw new Error(message);
    }

    return res.json().catch(() => ({ id, status: newStatus }));
}

export interface AdminUser {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    role: "GUEST" | "OWNER" | "ADMIN";
    status: string;
    createdAt?: string;
    propertyCount: number;
}

export interface AdminUserKpis {
    totalUsers: number;
    totalGuests: number;
    totalOwners: number;
    totalProperties: number;
}

export interface PaginatedResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

const mockDemoUsers: AdminUser[] = [
    { id: "1", firstName: "Alice", lastName: "Morgan", email: "guest.demo@kwatera.local", role: "GUEST", status: "Active", createdAt: "2026-01-15T10:00:00Z", propertyCount: 0 },
    { id: "2", firstName: "Marcus", lastName: "Green", email: "owner.demo@kwatera.local", role: "OWNER", status: "Active", createdAt: "2026-01-20T12:30:00Z", propertyCount: 5 },
    { id: "3", firstName: "Olivia", lastName: "Stone", email: "admin.demo@kwatera.local", role: "ADMIN", status: "Active", createdAt: "2026-01-01T08:00:00Z", propertyCount: 0 },
    { id: "4", firstName: "John", lastName: "Smith", email: "john.smith@gmail.com", role: "GUEST", status: "Active", createdAt: "2026-02-10T14:20:00Z", propertyCount: 0 },
    { id: "5", firstName: "Emily", lastName: "Brown", email: "emily.b@yahoo.com", role: "OWNER", status: "Active", createdAt: "2026-02-15T09:15:00Z", propertyCount: 3 },
    { id: "6", firstName: "Michael", lastName: "Davis", email: "m.davis@outlook.com", role: "GUEST", status: "Inactive", createdAt: "2026-03-01T11:45:00Z", propertyCount: 0 },
    { id: "7", firstName: "Sophia", lastName: "Wilson", email: "sophia.w@kwatera.local", role: "GUEST", status: "Active", createdAt: "2026-03-05T16:10:00Z", propertyCount: 0 },
    { id: "8", firstName: "James", lastName: "Taylor", email: "james.t@gmail.com", role: "OWNER", status: "Active", createdAt: "2026-03-10T10:00:00Z", propertyCount: 1 },
];

export async function getAdminUserKpis(): Promise<AdminUserKpis> {
    if (IS_DEMO_MODE) {
        return {
            totalUsers: 15,
            totalGuests: 10,
            totalOwners: 4,
            totalProperties: 9
        };
    }
    const token = localStorage.getItem("token");
    const res = await fetch(`${GATEWAY_BASE_URL}/api/admin/users/kpis`, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    if (!res.ok) throw new Error("Failed to fetch admin user KPIs");
    return res.json();
}

export async function getAdminUsers(page: number, size: number, role?: string, search?: string): Promise<PaginatedResponse<AdminUser>> {
    if (IS_DEMO_MODE) {
        let filtered = [...mockDemoUsers];
        if (role && role !== "ALL") {
            filtered = filtered.filter(u => u.role === role);
        }
        if (search) {
            const query = search.toLowerCase();
            filtered = filtered.filter(u => 
                u.firstName.toLowerCase().includes(query) ||
                u.lastName.toLowerCase().includes(query) ||
                u.email.toLowerCase().includes(query)
            );
        }
        const totalElements = filtered.length;
        const totalPages = Math.ceil(totalElements / size);
        const start = page * size;
        const end = start + size;
        const content = filtered.slice(start, end);
        
        return {
            content,
            totalPages,
            totalElements,
            size,
            number: page
        };
    }
    
    const token = localStorage.getItem("token");
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());
    if (role && role !== "ALL") params.append("role", role);
    if (search) params.append("search", search);
    
    const res = await fetch(`${GATEWAY_BASE_URL}/api/admin/users?${params.toString()}`, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    if (!res.ok) throw new Error("Failed to fetch admin users");
    return res.json();
}
