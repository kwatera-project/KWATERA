import { GATEWAY_BASE_URL } from "./apiConfig";

export interface UserProfile {
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    role: string;
}

export async function getUserProfile(): Promise<UserProfile> {
    const token = localStorage.getItem("token");
    if (!token) {
        throw new Error("No token found");
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/auth/users/me`, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });

    if (!res.ok) {
        throw new Error("Failed to fetch user profile");
    }

    return res.json();
}

export async function updateUserProfile(firstName: string, lastName: string): Promise<UserProfile> {
    const token = localStorage.getItem("token");
    if (!token) {
        throw new Error("No token found");
    }

    const res = await fetch(`${GATEWAY_BASE_URL}/api/auth/users/me`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ firstName, lastName })
    });

    if (!res.ok) {
        throw new Error("Failed to update user profile");
    }

    return res.json();
}
