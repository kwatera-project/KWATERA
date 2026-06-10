import { useEffect, useMemo, useState } from "react";
import { AlertCircle, FileClock, Loader2, Search } from "lucide-react";
import { getSystemEvents } from "../api/adminApi";
import type { SystemEvent, SystemEventType } from "../api/adminApi";

const ACTION_TYPES: Array<SystemEventType | "ALL"> = [
  "ALL",
  "RESERVATION_CREATED",
  "MANUAL_RESERVATION_CREATED",
  "UNIT_BLOCKED",
  "RESERVATION_STATUS_CHANGED",
  "EXPIRED_RESERVATION_CANCELLED"
];

type SystemEventsLoadState = {
  actionType: SystemEventType | "ALL" | null;
  events: SystemEvent[];
  error: string | null;
};

function formatAction(actionType: string) {
  return actionType
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(timestamp: string) {
  return new Intl.DateTimeFormat("pl-PL", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(timestamp));
}

function compactId(value: string | null) {
  return value ? value.slice(0, 8) : "-";
}

export default function AdminSystemEventsPage() {
  const [loadState, setLoadState] = useState<SystemEventsLoadState>({
    actionType: null,
    events: [],
    error: null
  });
  const [search, setSearch] = useState("");
  const [actionType, setActionType] = useState<SystemEventType | "ALL">("ALL");
  const [sortDirection, setSortDirection] = useState<"desc" | "asc">("desc");

  const loading = loadState.actionType !== actionType;
  const events = useMemo(() => (loading ? [] : loadState.events), [loadState.events, loading]);
  const error = loading ? null : loadState.error;

  useEffect(() => {
    let cancelled = false;
    const requestedActionType = actionType;

    getSystemEvents(requestedActionType)
      .then((data) => {
        if (!cancelled) {
          setLoadState({
            actionType: requestedActionType,
            events: data,
            error: null
          });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setLoadState({
            actionType: requestedActionType,
            events: [],
            error: err instanceof Error ? err.message : "Failed to fetch system events"
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [actionType]);

  const filteredEvents = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return events
      .filter((event) => {
        if (!normalizedSearch) {
          return true;
        }
        return [
          event.actionType,
          event.actorUserId ?? "System",
          event.entityType ?? "",
          event.entityId ?? "",
          event.details ?? ""
        ]
          .join(" ")
          .toLowerCase()
          .includes(normalizedSearch);
      })
      .sort((left, right) => {
        const result =
          new Date(left.timestamp).getTime() - new Date(right.timestamp).getTime();
        return sortDirection === "asc" ? result : -result;
      });
  }, [events, search, sortDirection]);

  return (
    <div className="space-y-5">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div>
          <h2 className="text-xl font-extrabold text-[#1A1A1A]">System Logs</h2>
          <p className="text-sm text-[#7A7A7A]">
            Admin-only reservation and system event history.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <label className="relative block">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#7A7A7A]" />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search logs"
              className="w-full sm:w-72 rounded-lg border border-[#DACDCA] bg-white pl-9 pr-3 py-2 text-sm font-medium text-[#1A1A1A] outline-none focus:ring-2 focus:ring-[#42211D]/20"
            />
          </label>
          <select
            value={actionType}
            onChange={(event) => setActionType(event.target.value as SystemEventType | "ALL")}
            className="rounded-lg border border-[#DACDCA] bg-white px-3 py-2 text-sm font-bold text-[#1A1A1A] outline-none focus:ring-2 focus:ring-[#42211D]/20"
          >
            {ACTION_TYPES.map((type) => (
              <option key={type} value={type}>
                {type === "ALL" ? "All actions" : formatAction(type)}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-lg text-red-700 shadow-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span className="text-sm font-medium">{error}</span>
        </div>
      )}

      <div className="bg-white border border-[#DACDCA] rounded-lg shadow-sm overflow-hidden">
        {loading ? (
          <div className="h-64 flex items-center justify-center text-[#42211D]">
            <Loader2 className="w-7 h-7 animate-spin" />
          </div>
        ) : filteredEvents.length === 0 ? (
          <div className="h-64 flex flex-col items-center justify-center text-center px-6">
            <FileClock className="w-10 h-10 text-[#DACDCA] mb-3" />
            <p className="text-sm font-bold text-[#1A1A1A]">No system events found</p>
            <p className="text-xs text-[#7A7A7A] mt-1">
              Adjust the search or action filter to inspect a different slice.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-[#DACDCA]">
              <thead className="bg-[#F7F7F7]">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
                    <button
                      type="button"
                      onClick={() =>
                        setSortDirection((current) => (current === "desc" ? "asc" : "desc"))
                      }
                      className="font-black uppercase tracking-wider hover:text-[#42211D]"
                    >
                      Date {sortDirection === "desc" ? "Newest" : "Oldest"}
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
                    Action
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
                    Actor
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
                    Entity
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
                    Details
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F1F1F1]">
                {filteredEvents.map((event) => (
                  <tr key={event.id} className="hover:bg-[#F7F7F7]/70">
                    <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-[#1A1A1A]">
                      {formatDate(event.timestamp)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span className="inline-flex rounded-full border border-[#DACDCA] bg-[#42211D]/5 px-2.5 py-1 text-xs font-bold text-[#42211D]">
                        {formatAction(event.actionType)}
                      </span>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-[#1A1A1A]">
                      {event.actorUserId ? (
                        <span title={event.actorUserId}>{compactId(event.actorUserId)}</span>
                      ) : (
                        "System"
                      )}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-[#1A1A1A]">
                      <div className="font-bold">{event.entityType ?? "-"}</div>
                      <div className="text-xs text-[#7A7A7A]" title={event.entityId ?? undefined}>
                        {compactId(event.entityId)}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-[#1A1A1A] min-w-80">
                      {event.details ?? "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
