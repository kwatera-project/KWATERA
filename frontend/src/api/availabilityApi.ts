const API_URL = "http://localhost:8080/api";

export async function checkAvailability(unitId: string, from: string, to: string) {
    const res = await fetch(
        `${API_URL}/availability?unitId=${unitId}&from=${from}&to=${to}`
    );
    return res.json();
}