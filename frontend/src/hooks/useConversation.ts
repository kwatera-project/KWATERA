import { useEffect, useRef, useState } from "react";
import { getConversation, getMessageHistory, sendMessage } from "../api/conversationApi";
import { GATEWAY_BASE_URL, IS_DEMO_MODE } from "../api/apiConfig";
import { connectConversationSocket, conversationSocketUrl } from "../api/conversationSocket";
import { createConversationSession, emptyConversation } from "../api/conversationSession";
import { getReservationDetails } from "../api/reservationApi";
import { subscribeAuthChanges } from "../utils/authSession";
import { decodeJwt } from "../utils/jwtUtils";

export function useConversation(conversationId: string, userId: string | undefined, token: string | null) {
    const [state, setState] = useState(emptyConversation);
    const sessionRef = useRef<ReturnType<typeof createConversationSession> | null>(null);

    useEffect(() => {
        if (IS_DEMO_MODE) return;
        const isCurrent = () => localStorage.getItem("token") === token;
        const exp = decodeJwt(token)?.exp;
        const session = createConversationSession({
            conversationId, userId, isCurrent, onChange: setState,
            getConversation, getHistory: getMessageHistory, sendMessage, getReservation: getReservationDetails,
            connect: callbacks => connectConversationSocket({
                url: conversationSocketUrl(GATEWAY_BASE_URL, window.location.origin),
                conversationId, token: token ?? "", isCurrent,
                expiresAt: typeof exp === "number" ? exp * 1000 : undefined,
                ...callbacks,
            }),
        });
        sessionRef.current = session;
        const unsubscribe = subscribeAuthChanges(() => {
            if (!isCurrent()) session.stop();
        });
        void session.start();
        return () => {
            unsubscribe();
            session.stop();
            if (sessionRef.current === session) sessionRef.current = null;
        };
    }, [conversationId, userId, token]);

    return {
        ...state,
        loadHistory: (action: "refresh" | "older") => sessionRef.current?.loadHistory(action),
        send: (content: string) => sessionRef.current?.send(content) ?? Promise.resolve(false),
        retryContext: () => sessionRef.current?.retryContext(),
    };
}
