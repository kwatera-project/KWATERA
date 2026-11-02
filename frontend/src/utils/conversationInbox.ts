import type { ConversationResponse } from "../types/conversation";

export const CONVERSATION_ROLES = ["ROLE_GUEST", "ROLE_OWNER"];
export const INBOX_PAGE_SIZE = 10;

export function canUseConversations(roles: string[]): boolean {
    return roles.some(role => CONVERSATION_ROLES.includes(role));
}

export function conversationAccessError(status: number | null) {
    if (status === 401) return "unauthenticated";
    if (status === 403 || status === 404) return "inaccessible";
    if (status === 409 || status === 410) return "unavailable";
    return null;
}

export function conversationActivity(conversation: ConversationResponse): string | null {
    for (const timestamp of [conversation.lastMessageAt, conversation.createdAt]) {
        if (timestamp && Number.isFinite(Date.parse(timestamp))) return timestamp;
    }
    return null;
}

export function sortConversations(conversations: ConversationResponse[]): ConversationResponse[] {
    return [...conversations].sort((a, b) => {
        const aTime = conversationActivity(a);
        const bTime = conversationActivity(b);
        return (bTime ? Date.parse(bTime) : -Infinity) - (aTime ? Date.parse(aTime) : -Infinity)
            || a.id.localeCompare(b.id);
    });
}
