export interface CreateReviewRequest {
    reservationId: string;
    rating: number;
    comment?: string | null;
}

export interface UpdateReviewRequest {
    rating?: number | null;
    comment?: string | null;
}

export interface ReviewResponseDto {
    id: string;
    reservationId: string;
    authorId: string;
    rating: number;
    comment?: string | null;
    createdAt: string | null;
    updatedAt?: string | null;
}

export interface PublicReviewDto {
    id: string;
    unitId: string;
    rating: number;
    comment?: string | null;
    createdAt: string;
    updatedAt?: string | null;
}

export interface ReviewPageDto {
    reviews: PublicReviewDto[];
    page: number;
    size: number;
    totalPages: number;
    reviewCount: number;
    averageRating: number;
}
