import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Loader2, MessageSquare } from "lucide-react";
import { ConversationApiError, getOrCreateConversation } from "../api/conversationApi";
import { decodeJwt, getUserRoles } from "../utils/jwtUtils";
import { canOpenReservationConversation, conversationAccessError } from "../utils/conversationInbox";
import { IS_DEMO_MODE } from "../api/apiConfig";
import { useAuthToken } from "../hooks/useAuthToken";

export default function ReservationConversationButton({ reservationId, status, guestUserId }: {
    reservationId: string;
    status: string;
    guestUserId?: string;
}) {
    const token = useAuthToken();
    if (IS_DEMO_MODE || !token || !canOpenReservationConversation(getUserRoles(token), decodeJwt(token)?.userId, guestUserId)) return null;
    return <ConversationAction key={`${token}:${reservationId}`} token={token} reservationId={reservationId} status={status} />;
}

function ConversationAction({ token, reservationId, status }: {
    token: string;
    reservationId: string;
    status: string;
}) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const busy = useRef(false);
    const alive = useRef(true);

    useEffect(() => {
        alive.current = true;
        return () => { alive.current = false; };
    }, []);

    // Terminal statuses may still be available within retention; the backend decides.
    const unavailable = !["PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"].includes(status);

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
                setError(cause instanceof ConversationApiError ? conversationAccessError(cause.status) ?? "open" : "open");
            }
        } finally {
            busy.current = false;
            if (alive.current) setLoading(false);
        }
    }

    return (
        <div className="min-w-0 max-w-sm space-y-2">
            <button type="button" onClick={() => void openConversation()} disabled={loading || unavailable}
                aria-busy={loading} title={unavailable ? t("inbox.errors.unavailable") : undefined}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 text-xs font-bold text-brand-primary bg-brand-bg border border-brand-accent rounded-lg shadow-sm hover:bg-brand-accent/30 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary disabled:opacity-50 disabled:cursor-not-allowed">
                {loading ? <Loader2 size={14} className="animate-spin motion-reduce:animate-none" aria-hidden="true" /> : <MessageSquare size={14} aria-hidden="true" />}
                {t(loading ? "inbox.opening" : "inbox.open")}
            </button>
            {(error || unavailable) && <p role={error ? "alert" : "status"} className="text-xs text-red-700 font-medium text-left">
                {t(`inbox.errors.${error ?? "unavailable"}`)}
                {error === "unauthenticated" && <> <Link to="/login" className="underline">{t("chat.login")}</Link></>}
            </p>}
        </div>
    );
}
