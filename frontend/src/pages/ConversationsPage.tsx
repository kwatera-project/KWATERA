import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { Link, Outlet, useLocation, useMatch, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ChevronRight, Loader2, MessageSquare, PanelLeftClose } from "lucide-react";
import type { AccessError } from "../api/conversationSession";
import { ConversationApiError, getConversations } from "../api/conversationApi";
import { getReservationDetails } from "../api/reservationApi";
import type { ConversationResponse } from "../types/conversation";
import { decodeJwt } from "../utils/jwtUtils";
import { conversationAccessError, conversationActivity, INBOX_PAGE_SIZE, sortConversations } from "../utils/conversationInbox";
import { getLocaleCode } from "../utils/locale";
import { useAuthToken } from "../hooks/useAuthToken";

const button = "chat-secondary";

export interface ConversationsOutletContext {
    reportAccess: (id: string, error: AccessError) => void;
    inboxCollapsed: boolean;
    toggleInbox: () => void;
}

export default function ConversationsPage() {
    const token = useAuthToken();
    return <ConversationsWorkspace key={token} token={token} />;
}

function ConversationsWorkspace({ token }: { token: string | null }) {
    const { t } = useTranslation();
    const selected = useMatch("/conversations/:conversationId")?.params.conversationId;
    const workspace = useRef<HTMLDivElement>(null);
    useLayoutEffect(() => {
        const panel = workspace.current;
        if (!panel) return;
        const viewport = window.visualViewport;
        const update = () => {
            const height = viewport?.scale === 1 ? viewport.height : window.innerHeight;
            const keyboard = Boolean(viewport && viewport.scale === 1 &&
                window.innerHeight - height > 120 && window.matchMedia("(max-width: 767px)").matches);
            panel.toggleAttribute("data-keyboard", keyboard);
            const top = keyboard ? viewport?.offsetTop ?? 0 : panel.getBoundingClientRect().top + window.scrollY;
            panel.style.setProperty("--chat-height", `${keyboard ? height : height - top}px`);
            panel.style.setProperty("--chat-top", `${top}px`);
        };
        const parent = panel.parentElement;
        const observer = new ResizeObserver(update);
        if (parent?.previousElementSibling) observer.observe(parent.previousElementSibling);
        update();
        window.addEventListener("resize", update);
        viewport?.addEventListener("resize", update);
        viewport?.addEventListener("scroll", update);
        return () => {
            observer.disconnect();
            window.removeEventListener("resize", update);
            viewport?.removeEventListener("resize", update);
            viewport?.removeEventListener("scroll", update);
        };
    }, []);

    const [attempt, setAttempt] = useState(0);
    const [collapseInbox, setCollapseInbox] = useState(false);
    const inboxCollapsed = Boolean(selected && collapseInbox);
    const focusInboxToggle = useRef(false);
    const toggleInbox = () => {
        focusInboxToggle.current = true;
        setCollapseInbox(value => !value);
    };
    useLayoutEffect(() => {
        if (!focusInboxToggle.current) return;
        focusInboxToggle.current = false;
        workspace.current?.querySelector<HTMLElement>("[data-inbox-toggle]")?.focus({ preventScroll: true });
    }, [inboxCollapsed]);
    const [sessionDenied, setSessionDenied] = useState(false);
    const [deniedIds, setDeniedIds] = useState<ReadonlySet<string>>(() => new Set());
    const reportAccess = useCallback((id: string, error: AccessError) => {
        if (error === "unauthenticated") setSessionDenied(true);
        else setDeniedIds(previous => previous.has(id) ? previous : new Set([...previous, id]));
    }, []);
    return (
        <div ref={workspace} className="chat-workspace mx-auto max-w-[96rem] bg-stone-50 text-brand-main md:px-5 md:py-4">
            <div className={`grid h-full min-h-0 min-w-0 ${inboxCollapsed ? "grid-cols-[minmax(0,1fr)]" : "md:grid-cols-[clamp(15rem,27vw,21rem)_minmax(0,1fr)] md:gap-4"}`}>
                <aside id="conversation-inbox" aria-label={t("inbox.title")} className={`min-h-0 min-w-0 flex-col bg-card md:rounded-xl md:border md:border-brand-accent ${selected ? inboxCollapsed ? "hidden" : "hidden md:flex" : "flex"}`}>
                    {!sessionDenied ? <Inbox key={attempt} token={token} userId={decodeJwt(token)?.userId} selected={selected} deniedIds={deniedIds} onCollapse={inboxCollapsed ? undefined : toggleInbox} onRetry={() => setAttempt(value => value + 1)} />
                        : !selected && <div className="space-y-3 p-4">
                            <h1 className="text-xl font-extrabold md:text-2xl">{t("inbox.title")}</h1>
                            <p role="alert" className="chat-feedback-error">{t("inbox.errors.unauthenticated")}</p>
                            <Link to="/login" className={button}>{t("chat.login")}</Link>
                        </div>}
                </aside>
                <div className={`min-h-0 min-w-0 bg-card md:rounded-xl md:border md:border-brand-accent ${selected ? "flex flex-col" : "hidden md:grid md:place-items-center"}`}>
                    {selected ? <Outlet context={{ reportAccess, inboxCollapsed, toggleInbox } satisfies ConversationsOutletContext} /> : <div className="p-6 text-center text-base text-brand-muted">
                        <MessageSquare size={28} className="mx-auto mb-3 text-brand-primary" aria-hidden="true" />
                        <p>{t("inbox.select")}</p>
                    </div>}
                </div>
            </div>
        </div>
    );
}

function Inbox({ token, userId, selected, deniedIds, onCollapse, onRetry }: { selected?: string; deniedIds: ReadonlySet<string>; token: string | null; userId: string | undefined; onCollapse?: () => void; onRetry: () => void }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const fromInbox = useLocation().state?.fromInbox === true;
    const [conversations, setConversations] = useState<ConversationResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [page, setPage] = useState(0);
    const [initialSelection] = useState(selected);
    const listRef = useRef<HTMLDivElement>(null);
    const entry = useRef<{ id: string; scrollTop: number } | null>(null);
    const available = useMemo(() => conversations.filter(item => !deniedIds.has(item.id)), [conversations, deniedIds]);
    const lastPage = Math.max(0, Math.ceil(available.length / INBOX_PAGE_SIZE) - 1);
    const currentPage = Math.min(page, lastPage);
    const visibleItems = useMemo(() => available.slice(currentPage * INBOX_PAGE_SIZE, (currentPage + 1) * INBOX_PAGE_SIZE), [available, currentPage]);

    useLayoutEffect(() => {
        const list = listRef.current;
        if (selected || !entry.current || !list) return;
        const { id, scrollTop } = entry.current;
        list.querySelector<HTMLElement>(`[data-conversation-id="${CSS.escape(id)}"]`)?.focus({ preventScroll: true });
        list.scrollTop = scrollTop;
    }, [selected]);

    useEffect(() => {
        let active = true;
        const isCurrentSession = () => active && localStorage.getItem("token") === token;
        async function load() {
            try {
                if (!userId) throw new ConversationApiError("Unauthorized", 401);
                const items = await getConversations();
                if (isCurrentSession()) {
                    const participants = sortConversations(items.filter(item => item.guestUserId === userId || item.ownerUserId === userId));
                    setConversations(participants);
                    const index = participants.findIndex(item => item.id === initialSelection);
                    if (index >= 0) setPage(Math.floor(index / INBOX_PAGE_SIZE));
                }
            } catch (cause) {
                if (isCurrentSession()) {
                    setConversations([]);
                    setError(cause instanceof ConversationApiError ? conversationAccessError(cause.status) ?? "load" : "load");
                }
            } finally {
                if (isCurrentSession()) setLoading(false);
            }
        }
        void load();
        return () => { active = false; };
    }, [token, userId, initialSelection]);

    return (
        <>
            <header className="flex shrink-0 items-center justify-between gap-2 border-b border-brand-accent p-4">
                <h1 className="text-xl font-extrabold tracking-tight md:text-2xl">
                    {selected ? <Link to="/conversations" replace title={t("inbox.back")} aria-label={t("inbox.back")}
                        className="rounded-sm hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary"
                        onClick={event => {
                            if (fromInbox && event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey) {
                                event.preventDefault();
                                navigate(-1);
                            }
                        }}>{t("inbox.title")}</Link> : t("inbox.title")}
                </h1>
                {selected && onCollapse && <button type="button" className={`${button} w-11 px-0`}
                    data-inbox-toggle aria-label={t("inbox.collapse")} title={t("inbox.collapse")}
                    aria-controls="conversation-inbox" aria-expanded onClick={onCollapse}>
                    <PanelLeftClose size={18} aria-hidden="true" />
                </button>}
            </header>
            <div ref={listRef} className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-3">
                {loading ? <p role="status" className="flex items-center gap-2 text-brand-muted"><Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />{t("inbox.loading")}</p>
                    : error ? <div className="bg-card border border-brand-accent rounded-xl p-6 space-y-4">
                        <p role="alert" className="chat-feedback-error">{t(`inbox.errors.${error}`)}</p>
                        {error === "unauthenticated" ? <Link to="/login" className={button}>{t("chat.login")}</Link>
                            : <button type="button" className={button} onClick={onRetry}>{t("chat.retry")}</button>}
                    </div>
                    : available.length === 0 ? <div className="bg-card border border-brand-accent rounded-xl p-6 sm:p-10 text-center space-y-3">
                        <MessageSquare size={28} className="mx-auto text-brand-primary" aria-hidden="true" />
                        <h2 className="text-lg font-bold">{t("inbox.empty")}</h2>
                        <p className="text-sm text-brand-muted">{t("inbox.emptyHint")}</p>
                    </div> : <>
                        <InboxItems key={currentPage} token={token} conversations={visibleItems} selected={selected}
                            onOpen={id => { if (!selected) entry.current = { id, scrollTop: listRef.current?.scrollTop ?? 0 }; }} />
                        {lastPage > 0 && <nav aria-label={t("inbox.pagination")} className="mt-3 flex flex-wrap items-center justify-between gap-2">
                            <button type="button" className={button} disabled={currentPage === 0} onClick={() => { listRef.current?.scrollTo(0, 0); setPage(currentPage - 1); }}>{t("inbox.previous")}</button>
                            <p role="status" className="text-sm text-brand-muted">{t("inbox.page", { page: currentPage + 1, total: lastPage + 1 })}</p>
                            <button type="button" className={button} disabled={currentPage === lastPage} onClick={() => { listRef.current?.scrollTo(0, 0); setPage(currentPage + 1); }}>{t("inbox.next")}</button>
                        </nav>}
                    </>}
            </div>
        </>
    );
}

function InboxItems({ token, conversations, selected, onOpen }: { token: string | null; conversations: ConversationResponse[]; selected?: string; onOpen: (id: string) => void }) {
    const { t, i18n } = useTranslation();
    const fromInbox = useLocation().state?.fromInbox === true;
    const [unitNames, setUnitNames] = useState<Record<string, string>>({});

    useEffect(() => {
        let active = true;
        const isCurrentSession = () => active && localStorage.getItem("token") === token;
        async function enrich() {
            // Only the visible page, at most three requests in flight. A failed
            // optional context lookup must never hide an available conversation.
            const ids = [...new Set(conversations.map(item => item.reservationId))];
            for (let offset = 0; offset < ids.length && isCurrentSession(); offset += 3) {
                const results = await Promise.all(ids.slice(offset, offset + 3).map(async id => {
                    try {
                        const details = await getReservationDetails(id);
                        return [id, typeof details.unitName === "string" ? details.unitName : ""] as const;
                    } catch {
                        return [id, ""] as const;
                    }
                }));
                if (isCurrentSession()) setUnitNames(previous => ({ ...previous, ...Object.fromEntries(results) }));
            }
        }
        void enrich();
        return () => { active = false; };
    }, [token, conversations]);

    return <ul aria-label={t("inbox.title")} className="space-y-2">
        {conversations.map(conversation => {
            const activity = conversationActivity(conversation);
            const current = selected === conversation.id;
            return <li key={conversation.id}>
                <Link to={`/conversations/${conversation.id}`} state={{ fromInbox: !selected || fromInbox }} replace={Boolean(selected && fromInbox)} aria-current={current ? "page" : undefined}
                    data-conversation-id={conversation.id}
                    onClick={event => { if (!event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey) onOpen(conversation.id); }}
                    className={`flex items-center gap-2 rounded-lg border px-3 py-2.5 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-brand-primary ${current ? "border-brand-primary bg-brand-bg ring-1 ring-brand-primary" : "border-brand-accent hover:bg-brand-bg"}`}>
                    <div className="min-w-0 flex-1 space-y-1">
                        <h2 className="font-bold [overflow-wrap:anywhere]">{unitNames[conversation.reservationId] || t("reservationDetails.nameUnavailable")}</h2>
                        <p className="text-sm text-brand-muted">
                            <span title={conversation.reservationId} className="font-mono font-bold">#RES-{conversation.reservationId.slice(-8)}</span>
                            {" · "}{t(`statuses.${conversation.reservationStatus}`)}
                        </p>
                        {activity && <p className="text-sm text-brand-muted">
                            {t("inbox.lastActivity")}{" "}<time dateTime={activity}>{new Date(activity).toLocaleString(getLocaleCode(i18n.language), { dateStyle: "medium", timeStyle: "short" })}</time>
                        </p>}
                    </div>
                    <ChevronRight size={18} className="shrink-0 text-brand-primary" aria-hidden="true" />
                </Link>
            </li>;
        })}
    </ul>;
}
