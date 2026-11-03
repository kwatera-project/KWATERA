import {GATEWAY_BASE_URL} from "./apiConfig.ts";
import type {CreateReviewRequest, ReviewPageDto, ReviewResponseDto, UpdateReviewRequest} from "../types/review.ts";

const API_URL = `${GATEWAY_BASE_URL}/api/reviews`;

export async function getReviewsForUnit(
    unitId: string,
    page: number = 0,
    size: number = 10
): Promise<ReviewPageDto> {
    const res = await fetch(`${API_URL}/units/${unitId}?page=${page}&size=${size}`, {
        method: "GET",
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch reviews for unit");
    }

    return res.json();
}

export async function getReviewsForProperty(
    propertyId: string,
    page: number = 0,
    size: number = 10
): Promise<ReviewPageDto> {
    const res = await fetch(`${API_URL}/properties/${propertyId}?page=${page}&size=${size}`, {
        method: "GET",
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch reviews for property");
    }

    return res.json();
}

export async function createReview(
    reservationId: string,
    rating: number,
    comment?: string
): Promise<ReviewResponseDto> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to write a review");
    }

    const res = await fetch(API_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            reservationId,
            rating,
            comment,
        } as CreateReviewRequest),
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to create review");
    }

    return res.json();
}

export async function getMyReviews(): Promise<ReviewResponseDto[]> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view your reviews");
    }

    const res = await fetch(`${API_URL}/my`, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`
        },
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch your reviews");
    }

    return res.json();
}

export async function getReviewDetails(reviewId: string): Promise<ReviewResponseDto> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to view review details");
    }

    const res = await fetch(`${API_URL}/${reviewId}`, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`
        },
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to fetch review details");
    }

    return res.json();
}

export async function updateReview(
    reviewId: string,
    rating?: number,
    comment?: string
): Promise<ReviewResponseDto> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to update your review");
    }

    const res = await fetch(`${API_URL}/${reviewId}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            rating,
            comment,
        } as UpdateReviewRequest),
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to update review");
    }

    return res.json();
}

export async function deleteReview(reviewId: string): Promise<void> {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error("Log in to delete a review");
    }

    const res = await fetch(`${API_URL}/${reviewId}`, {
        method: "DELETE",
        headers: {
            "Authorization": `Bearer ${token}`
        },
    });

    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Failed to delete review");
    }
}
