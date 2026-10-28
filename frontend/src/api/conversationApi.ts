import { GATEWAY_BASE_URL } from "./apiConfig";
import type {
    ConversationResponse,
    MessageHistoryParams,
    MessageHistoryResponse,
    MessageResponse,
    SendMessageRequest,
} from "../types/conversation";

export class ConversationApiError extends Error {
    readonly status: number | null;

    constructor(message: string, status: number | null, options?: ErrorOptions) {
        super(message, options);
        this.name = "ConversationApiError";
        this.status = status;
    }
}

async function request<T>(path: string, fallbackMessage: string, init?: RequestInit): Promise<T> {
    const token = localStorage.getItem("token");
    if (!token) {
        throw new ConversationApiError("Log in to access conversations", 401);
    }

    let response: Response;
    try {
        response = await fetch(`${GATEWAY_BASE_URL}${path}`, {
            ...init,
            headers: {
                ...init?.headers,
                Authorization: `Bearer ${token}`,
            },
        });
    } catch (cause) {
        throw new ConversationApiError("Unable to reach the conversation service", null, { cause });
    }

    if (!response.ok) {
        const errorData = await response.json().catch(() => null) as {
            message?: string;
            detail?: string;
            error?: string;
        } | null;
        const message = errorData?.message || errorData?.detail || errorData?.error;
        throw new ConversationApiError(
            typeof message === "string" ? message : `${fallbackMessage} (HTTP ${response.status})`,
            response.status,
        );
    }

    return response.json();
}

export async function getOrCreateConversation(reservationId: string): Promise<ConversationResponse> {
    return request(
        `/api/v1/reservations/${encodeURIComponent(reservationId)}/conversation`,
        "Failed to get or create conversation",
        { method: "PUT" },
    );
}

export async function getConversations(): Promise<ConversationResponse[]> {
    return request("/api/v1/conversations", "Failed to fetch conversations");
}

export async function getConversation(conversationId: string): Promise<ConversationResponse> {
    return request(
        `/api/v1/conversations/${encodeURIComponent(conversationId)}`,
        "Failed to fetch conversation",
    );
}

export async function sendMessage(
    conversationId: string,
    data: SendMessageRequest,
): Promise<MessageResponse> {
    return request(
        `/api/v1/conversations/${encodeURIComponent(conversationId)}/messages`,
        "Failed to send message",
        {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ content: data.content }),
        },
    );
}

export async function getMessageHistory(
    conversationId: string,
    params: MessageHistoryParams = {},
): Promise<MessageHistoryResponse> {
    const query = new URLSearchParams();
    if (params.limit !== undefined) {
        query.set("limit", params.limit.toString());
    }
    if (params.cursor !== undefined && params.cursor !== null) {
        query.set("cursor", params.cursor);
    }

    return request(
        `/api/v1/conversations/${encodeURIComponent(conversationId)}/messages${query.size ? `?${query}` : ""}`,
        "Failed to fetch message history",
    );
}
