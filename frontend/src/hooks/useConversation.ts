import { useEffect, useRef, useState } from "react";
import { ConversationApiError, getConversation, getMessageHistory, sendMessage } from "../api/conversationApi";
import { getReservationDetails } from "../api/reservationApi";
import type { ConversationResponse, MessageResponse } from "../types/conversation";
import type { ReservationDetails } from "../types/reservation";
import { mergeMessages } from "../utils/conversationMessages";

type AccessError = "unauthenticated" | "inaccessible" | "unavailable";
type Operation = "refresh" | "older" | "send";

function accessError(error: unknown): AccessError | null {
    if (!(error instanceof ConversationApiError)) return null;
    if (error.status === 401) return "unauthenticated";
    if (error.status === 403 || error.status === 404) return "inaccessible";
    if (error.status === 409 || error.status === 410) return "unavailable";
    return null;
}

export function useConversation(conversationId: string, userId: string | undefined) {
    const [conversation, setConversation] = useState<ConversationResponse | null>(null);
    const [reservation, setReservation] = useState<ReservationDetails | null>(null);
    const [contextLoading, setContextLoading] = useState(false);
    const [messages, setMessages] = useState<MessageResponse[]>([]);
    const [nextCursor, setNextCursor] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [fatalError, setFatalError] = useState<AccessError | "load" | null>(null);
    const [operation, setOperation] = useState<Operation | null>(null);
    const [operationError, setOperationError] = useState<Operation | "invalidMessage" | null>(null);
    const alive = useRef(true);
    const busy = useRef(false);
    const newestSyncedId = useRef<string | null>(null);

    useEffect(() => {
        let active = true;
        alive.current = true;
        async function load() {
            try {
                if (!userId) throw new ConversationApiError("Unauthorized", 401);
                const details = await getConversation(conversationId);
                if (!active) return;
                if (userId !== details.guestUserId && userId !== details.ownerUserId) {
                    throw new ConversationApiError("Inaccessible", 404);
                }
                const [history, context] = await Promise.all([
                    getMessageHistory(conversationId),
                    getReservationDetails(details.reservationId).catch(() => null),
                ]);
                if (!active) return;
                newestSyncedId.current = history.items[0]?.id ?? null;
                setConversation(details);
                setReservation(context);
                setMessages(mergeMessages([], history.items));
                setNextCursor(history.hasMore ? history.nextCursor : null);
            } catch (error) {
                if (active) setFatalError(accessError(error) ?? "load");
            } finally {
                if (active) setLoading(false);
            }
        }
        void load();
        return () => {
            active = false;
            alive.current = false;
        };
    }, [conversationId, userId]);

    function handleFailure(error: unknown, action: Operation) {
        const fatal = accessError(error);
        if (fatal) {
            setConversation(null);
            setReservation(null);
            setMessages([]);
            setFatalError(fatal);
        } else {
            setOperationError(action === "send" && error instanceof ConversationApiError && error.status === 400
                ? "invalidMessage" : action);
        }
    }

    async function loadHistory(action: "refresh" | "older") {
        if (busy.current || !conversation || (action === "older" && !nextCursor)) return;
        busy.current = true;
        setOperation(action);
        setOperationError(null);
        try {
            if (action === "older") {
                const page = await getMessageHistory(conversationId, { cursor: nextCursor });
                if (!alive.current) return;
                setMessages(current => mergeMessages(current, page.items));
                setNextCursor(page.hasMore ? page.nextCursor : null);
            } else {
                let page = await getMessageHistory(conversationId);
                if (!alive.current) return;
                const newestId = page.items[0]?.id ?? null;
                const incoming = [...page.items];
                const anchor = newestSyncedId.current;
                while (page.hasMore && page.nextCursor && !page.items.some(message => message.id === anchor)) {
                    page = await getMessageHistory(conversationId, { cursor: page.nextCursor });
                    if (!alive.current) return;
                    incoming.push(...page.items);
                }
                if (!alive.current) return;
                newestSyncedId.current = newestId;
                setMessages(current => mergeMessages(current, incoming));
                if (!page.hasMore) setNextCursor(null);
            }
        } catch (error) {
            if (alive.current) handleFailure(error, action);
        } finally {
            busy.current = false;
            if (alive.current) setOperation(null);
        }
    }

    async function send(content: string): Promise<boolean> {
        if (busy.current || !conversation) return false;
        busy.current = true;
        setOperation("send");
        setOperationError(null);
        try {
            const message = await sendMessage(conversationId, { content });
            if (!alive.current) return false;
            setMessages(current => mergeMessages(current, [message]));
            return true;
        } catch (error) {
            if (alive.current) handleFailure(error, "send");
            return false;
        } finally {
            busy.current = false;
            if (alive.current) setOperation(null);
        }
    }

    async function retryContext() {
        if (!conversation || contextLoading) return;
        setContextLoading(true);
        try {
            const details = await getReservationDetails(conversation.reservationId);
            if (alive.current) setReservation(details);
        } catch {
            if (alive.current) setReservation(null);
        } finally {
            if (alive.current) setContextLoading(false);
        }
    }

    return {
        conversation, reservation, contextLoading, messages, nextCursor, loading,
        fatalError, operation, operationError, loadHistory, send, retryContext,
    };
}
