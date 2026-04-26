const API_URL = "http://localhost:8080/api/v1/reservations";

export async function createReservation(unitId: string, from: string, to: string) {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to book this unit");
    }

    const res = await fetch(API_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            unitId: unitId,
            startDate: from,
            endDate: to
        })
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to create reservation");
    }

    return res.json();
}