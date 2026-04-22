const API_URL = "http://localhost:8083/api";

export async function getProperties() {
    const res = await fetch(`${API_URL}/properties`);
    return res.json();
}

export async function getProperty(id: string) {
    const res = await fetch(`${API_URL}/properties/${id}`);
    return res.json();
}

export async function getUnits(propertyId: string) {
    const res = await fetch(`${API_URL}/properties/${propertyId}/units`);
    return res.json();
}