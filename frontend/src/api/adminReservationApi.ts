const API_URL = "http://localhost:8080/api/v1/admin/reservations";

export async function getAdminReservations(status?: string) {
    const token = localStorage.getItem("token");
    let url = API_URL;
    if (status) {
        url += `?status=${status}`;
    }

    const res = await fetch(url, {
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch admin reservations");
    }

    return res.json();
}

export async function updateReservationStatus(id: string, newStatus: string) {
    const token = localStorage.getItem("token");

    const res = await fetch(`${API_URL}/${id}/status`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            newStatus: newStatus
        })
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to update reservation status");
    }

    return res.json();
}
