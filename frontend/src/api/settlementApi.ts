import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "./apiConfig";
import { demoSettlementBySettlementId, demoSettlementsByReservationId } from "../demo/demoBilling";

const API_URL = `${GATEWAY_BASE_URL}/api/billing/settlements`;

export async function getSettlementDetails(id: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view this settlement");
    }

    if (IS_DEMO_MODE) {
        const settlement = demoSettlementsByReservationId[id] ?? demoSettlementBySettlementId[id];
        if (!settlement) throw new Error("Demo settlement not found");
        return settlement;
    }

    const res = await fetch(`${API_URL}/${id}`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch settlement details");
    }

    const data = await res.json();
    return {
        ...data.settlement,
        items: data.items
    };
}

export async function getSettlementItemInfoByType(id: string, type: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view this settlement");
    }

    if (IS_DEMO_MODE) {
        const settlement = demoSettlementsByReservationId[id] ?? demoSettlementBySettlementId[id];
        return settlement?.items?.find((item) => item.type === type) ?? null;
    }

    const res = await fetch(`${API_URL}/${id}/${type}`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch settlement details");
    }

    return res.json();
}

export async function downloadInvoice(reservationId: string): Promise<void> {
    const token = localStorage.getItem("token");
    if (!token) throw new Error("Log in to download invoice");

    if (IS_DEMO_MODE) {
        alert("Demo Mode: Invoice download is simulated.");
        return;
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/billing/settlements/${reservationId}/invoice`, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        throw new Error("Failed to download invoice");
    }

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `invoice-${reservationId}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
}
