import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Loader2, MessageSquare } from "lucide-react";
import { ConversationApiError, getOrCreateConversation } from "../api/conversationApi";
import { decodeJwt, getUserRoles } from "../utils/jwtUtils";
import { canOpenReservationConversation, conversationAccessError } from "../utils/conversationInbox";
import { IS_DEMO_MODE } from "../api/apiConfig";
import { useAuthToken } from "../hooks/useAuthToken";

export default function ReservationConversationButton({ reservationId, status, guestUserId, showReason = false }: {
    reservationId: string;
    status: string;
    guestUserId?: string;
    showReason?: boolean;
}) {
    const token = useAuthToken();
    if (IS_DEMO_MODE || !token || !canOpenReservationConversation(getUserRoles(token), decodeJwt(token)?.userId, guestUserId)) return null;
    return <ConversationAction key={`${token}:${reservationId}`} token={token} reservationId={reservationId} status={status} showReason={showReason} />;
}

function ConversationAction({ token, reservationId, status, showReason }: {
    token: string;
    showReason: boolean;
    reservationId: string;
    status: string;
}) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const statusRef = useRef<HTMLDivElement>(null);
    const [reason, setReason] = useState<string | null>(null);
    const busy = useRef(false);
    const alive = useRef(true);

    useEffect(() => {
        alive.current = true;
        return () => { alive.current = false; };
    }, []);

    // Terminal statuses may still be available within retention; the backend decides.
    const blocked = status === "BLOCKED";
    const unavailable = blocked || error === "unavailable" || error === "inaccessible";

    useLayoutEffect(() => {
        if (error === "unavailable" || error === "inaccessible") statusRef.current?.focus({ preventScroll: true });
    }, [error]);

    async function openConversation() {
        if (busy.current || unavailable) return;
        busy.current = true;
        setLoading(true);
        setError(null);
        try {
            const conversation = await getOrCreateConversation(reservationId);
            if (alive.current && localStorage.getItem("token") === token) {
                navigate(`/conversations/${conversation.id}`);
            }
        } catch (cause) {
            if (alive.current && localStorage.getItem("token") === token) {
                const accessError = cause instanceof ConversationApiError ? conversationAccessError(cause.status) : null;
                setError(accessError ?? "open");
                if (accessError === "unavailable" && cause instanceof ConversationApiError &&
                    cause.message === "Conversation retention period has expired") setReason("retention");
            }
        } finally {
            busy.current = false;
            if (alive.current) setLoading(false);
        }
    }

    return (
        <div className={`min-w-0 text-left ${showReason ? "max-w-[18rem]" : "w-40 max-w-full"}`}>
            {unavailable ? <div ref={statusRef} role="status" tabIndex={-1} className="rounded text-brand-primary focus-visible:outline-2 focus-visible:outline-brand-primary">
                <p className="flex min-h-11 items-center gap-2 text-xs font-bold">
                    <MessageSquare size={14} className="shrink-0" aria-hidden="true" />{t("inbox.unavailable")}
                </p>
                {showReason && (blocked || reason) && <p className="text-sm">{t(blocked ? "inbox.reasons.blocked" : "inbox.reasons.retention")}</p>}
            </div> : error === "unauthenticated" ? <>
                <p role="alert" className="chat-feedback-error">{t("inbox.errors.unauthenticated")}</p>
                <Link to="/login" className="chat-secondary w-full">{t("chat.login")}</Link>
            </> : <>
                <button type="button" onClick={() => void openConversation()} disabled={loading}
                    aria-busy={loading} className={`chat-secondary ${showReason ? "" : "w-full text-xs"}`}>
                    {loading ? <Loader2 size={14} className="shrink-0 animate-spin motion-reduce:animate-none" aria-hidden="true" />
                        : <MessageSquare size={14} className="shrink-0" aria-hidden="true" />}
                    {t(loading ? "inbox.opening" : error ? "inbox.retryOpen" : "inbox.open")}
                </button>
                {error && <p role="alert" className="chat-feedback-error mt-1">{t("inbox.errors.open")}</p>}
            </>}
        </div>
    );
}
