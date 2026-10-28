export interface ConversationResponse {
    id: string;
    reservationId: string;
    guestUserId: string;
    ownerUserId: string;
    reservationStatus: "PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED" | "BLOCKED";
    createdAt: string;
    lastMessageAt: string | null;
    retentionExpiresOn: string | null;
}

export interface SendMessageRequest {
    content: string;
}

export interface MessageResponse {
    id: string;
    conversationId: string;
    senderUserId: string;
    content: string;
    createdAt: string;
}

export interface MessageHistoryResponse {
    items: MessageResponse[];
    nextCursor: string | null;
    hasMore: boolean;
}

export interface MessageHistoryParams {
    limit?: number;
    cursor?: string | null;
}
