import type { ConversationResponse } from "../types/conversation";

export const INBOX_PAGE_SIZE = 10;

export function canOpenReservationConversation(roles: string[], userId: string | undefined, guestUserId?: string): boolean {
    if (!userId) return false;
    if (userId === guestUserId) return true;
    // Admin reservation views include unrelated bookings. Only offer an action
    // when the available context proves participation; the inbox covers owners
    // snapshotted in a conversation, whose IDs are absent from reservation DTOs.
    if (roles.includes("ROLE_ADMIN")) return false;
    return roles.includes("ROLE_GUEST") || roles.includes("ROLE_OWNER");
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
        const milliseconds = (bTime ? Date.parse(bTime) : -Infinity) - (aTime ? Date.parse(aTime) : -Infinity);
        if (milliseconds) return milliseconds;
        // Date.parse truncates ISO instants to milliseconds. Compare the padded
        // fraction before the UUID tie break to retain backend sub-ms precision.
        const fraction = (time: string | null) => (time?.match(/\.(\d+)/)?.[1] ?? "").padEnd(9, "0");
        const aFraction = fraction(aTime);
        const bFraction = fraction(bTime);
        return (aFraction < bFraction ? 1 : aFraction > bFraction ? -1 : 0) || a.id.localeCompare(b.id);
    });
}
