import { useEffect, useMemo, useRef, useState } from "react";
import DatePicker from "react-datepicker";
import { AlertCircle, FileClock, Loader2, Search, X } from "lucide-react";
import { getSystemEvents } from "../api/adminApi";
import type { SystemEvent, SystemEventType } from "../api/adminApi";
import SharedDatePicker from "../components/SharedDatePicker";

const ACTION_TYPES: Array<SystemEventType | "ALL"> = [
  "ALL",
  "RESERVATION_CREATED",
  "MANUAL_RESERVATION_CREATED",
  "UNIT_BLOCKED",
  "RESERVATION_STATUS_CHANGED",
  "EXPIRED_RESERVATION_CANCELLED",
  "OCR_READING_ATTEMPTED",
  "OCR_READING_SUCCEEDED",
  "OCR_READING_FAILED",
  "METER_READING_MANUALLY_CORRECTED",
  "MEDIA_SETTLEMENT_GENERATED",
  "PAYMENT_FAILED",
  "PAYMENT_CANCELLED",
  "BALANCE_CHANGED"
];

type SystemEventsLoadState = {
  requestKey: string | null;
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

function buildLocalIso(
  date: Date | null,
  time: string,
  fallbackTime: string,
  endOfMinute = false
) {
  if (!date) {
    return null;
  }

  const [hour, minute] = (time || fallbackTime).split(":").map(Number);
  const localDate = new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    hour,
    minute,
    endOfMinute ? 59 : 0,
    endOfMinute ? 999 : 0
  );
  return localDate.toISOString();
}

function getActionBadgeClass(actionType: SystemEventType) {
  switch (actionType) {
    case "RESERVATION_CREATED":
      return "border-emerald-200 bg-emerald-50 text-emerald-800";
    case "MANUAL_RESERVATION_CREATED":
      return "border-indigo-200 bg-indigo-50 text-indigo-800";
    case "UNIT_BLOCKED":
      return "border-amber-200 bg-amber-50 text-amber-800";
    case "RESERVATION_STATUS_CHANGED":
      return "border-blue-200 bg-blue-50 text-blue-800";
    case "EXPIRED_RESERVATION_CANCELLED":
      return "border-rose-200 bg-rose-50 text-rose-800";
    case "OCR_READING_ATTEMPTED":
      return "border-slate-200 bg-slate-50 text-slate-800";
    case "OCR_READING_SUCCEEDED":
      return "border-teal-200 bg-teal-50 text-teal-800";
    case "OCR_READING_FAILED":
      return "border-red-200 bg-red-50 text-red-800";
    case "METER_READING_MANUALLY_CORRECTED":
      return "border-cyan-200 bg-cyan-50 text-cyan-800";
    case "MEDIA_SETTLEMENT_GENERATED":
      return "border-lime-200 bg-lime-50 text-lime-800";
    case "PAYMENT_FAILED":
      return "border-pink-200 bg-pink-50 text-pink-800";
    case "PAYMENT_CANCELLED":
      return "border-orange-200 bg-orange-50 text-orange-800";
    case "BALANCE_CHANGED":
      return "border-violet-200 bg-violet-50 text-violet-800";
    default:
      return "border-[#DACDCA] bg-[#42211D]/5 text-[#42211D]";
  }
}

export default function AdminSystemEventsPage() {
  const [loadState, setLoadState] = useState<SystemEventsLoadState>({
    requestKey: null,
    events: [],
    error: null
  });
  const [search, setSearch] = useState("");
  const [actionType, setActionType] = useState<SystemEventType | "ALL">("ALL");
  const [sortDirection, setSortDirection] = useState<"desc" | "asc">("desc");
  const [dateFrom, setDateFrom] = useState<Date | null>(null);
  const [timeFrom, setTimeFrom] = useState("");
  const [dateTo, setDateTo] = useState<Date | null>(null);
  const [timeTo, setTimeTo] = useState("");
  const dateToRef = useRef<DatePicker | null>(null);

  const range = useMemo(() => {
    const from = buildLocalIso(dateFrom, timeFrom, "00:00");
    const to = buildLocalIso(dateTo, timeTo, "23:59", true);
    const error =
      from && to && new Date(from).getTime() > new Date(to).getTime()
        ? "Date from must be before or equal to Date to."
        : null;
    return { from, to, error };
  }, [dateFrom, dateTo, timeFrom, timeTo]);

  const requestKey = useMemo(() => {
    if (range.error) {
      return null;
    }
    return JSON.stringify({
      actionType,
      from: range.from,
      to: range.to
    });
  }, [actionType, range.error, range.from, range.to]);

  const loading = requestKey !== null && loadState.requestKey !== requestKey;
  const events = useMemo(() => (loading ? [] : loadState.events), [loadState.events, loading]);
  const error = loading ? null : loadState.error;

  useEffect(() => {
    if (!requestKey) {
      return;
    }

    let cancelled = false;
    const requestedKey = requestKey;

    getSystemEvents({
      actionType,
      from: range.from,
      to: range.to
    })
      .then((data) => {
        if (!cancelled) {
          setLoadState({
            requestKey: requestedKey,
            events: data,
            error: null
          });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setLoadState({
            requestKey: requestedKey,
            events: [],
            error: err instanceof Error ? err.message : "Failed to fetch system events"
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [actionType, range.from, range.to, requestKey]);

  const filteredEvents = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return events
      .filter((event) => {
        if (!normalizedSearch) {
          return true;
        }
        return [
          event.actionType,
          formatAction(event.actionType),
          formatDate(event.timestamp),
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

  const clearDateFilters = () => {
    setDateFrom(null);
    setTimeFrom("");
    setDateTo(null);
    setTimeTo("");
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4">
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

        <div className="flex flex-wrap items-end gap-3 rounded-lg border border-[#DACDCA] bg-white p-3 shadow-sm">
          <div className="flex flex-col gap-1">
            <span className="text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
              Date from
            </span>
            <div className="flex items-center rounded-lg border border-[#DACDCA] bg-[#F7F7F7] px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-[#42211D]/20">
              <SharedDatePicker
                selected={dateFrom}
                onChange={(date: Date | null) => {
                  setDateFrom(date);
                  if (date && dateTo && date > dateTo) {
                    setDateTo(null);
                  }
                  if (date) {
                    setTimeout(() => {
                      dateToRef.current?.setOpen(true);
                    }, 100);
                  }
                }}
                selectsStart
                startDate={dateFrom}
                endDate={dateTo}
                placeholderText="Start"
                className="w-24 cursor-pointer bg-transparent text-center text-sm font-bold text-[#1A1A1A] outline-none"
                wrapperClassName="block"
                popperPlacement="bottom-start"
                allowPastDates={true}
              />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
              Date to
            </span>
            <div className="flex items-center rounded-lg border border-[#DACDCA] bg-[#F7F7F7] px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-[#42211D]/20">
              <SharedDatePicker
                datepickerRef={dateToRef}
                selected={dateTo}
                onChange={setDateTo}
                selectsEnd
                startDate={dateFrom}
                endDate={dateTo}
                minDate={dateFrom}
                placeholderText="End"
                className="w-24 cursor-pointer bg-transparent text-center text-sm font-bold text-[#1A1A1A] outline-none"
                wrapperClassName="block"
                popperPlacement="bottom-start"
                allowPastDates={true}
              />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
              Time from
            </span>
            <input
              type="time"
              value={timeFrom}
              onChange={(event) => setTimeFrom(event.target.value)}
              className="rounded-lg border border-[#DACDCA] bg-[#F7F7F7] px-3 py-2 text-sm font-semibold text-[#1A1A1A] outline-none focus:ring-2 focus:ring-[#42211D]/20"
            />
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-xs font-black uppercase tracking-wider text-[#7A7A7A]">
              Time to
            </span>
            <input
              type="time"
              value={timeTo}
              onChange={(event) => setTimeTo(event.target.value)}
              className="rounded-lg border border-[#DACDCA] bg-[#F7F7F7] px-3 py-2 text-sm font-semibold text-[#1A1A1A] outline-none focus:ring-2 focus:ring-[#42211D]/20"
            />
          </div>
          <button
            type="button"
            onClick={clearDateFilters}
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-[#DACDCA] bg-white px-4 py-2 text-sm font-bold text-[#42211D] transition-colors hover:bg-[#F7F7F7]"
          >
            <X className="h-4 w-4" />
            Clear dates
          </button>
          {range.error && (
            <p className="w-full text-sm font-semibold text-red-700">{range.error}</p>
          )}
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
              Adjust the search, action, or date filters to inspect a different slice.
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
                      <span
                        className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${getActionBadgeClass(event.actionType)}`}
                      >
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
