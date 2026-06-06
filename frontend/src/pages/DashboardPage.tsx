import { useState, useEffect, useRef, useCallback } from "react";
import { format } from "date-fns";
import { getDashboardReservationMetrics, getDashboardBillingMetrics } from "../api/adminApi";
import { getMyProperties } from "../api/ownerPropertyApi";
import { getPropertyUnits } from "../api/ownerUnitApi";
import { getProperties, getUnits } from "../api/propertyApi";
import { getSettlementDetails } from "../api/settlementApi";
import { getUserRoles } from "../utils/jwtUtils";
import { GATEWAY_BASE_URL } from "../api/apiConfig";
import SharedDatePicker from "../components/SharedDatePicker";
import DatePicker from "react-datepicker";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  LineChart,
  Line,
  BarChart,
  Bar,
  ComposedChart,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend
} from "recharts";
import type { TooltipPayloadEntry } from "recharts";
import type { Unit } from "../types/property";
import {
  Calendar,
  DollarSign,
  Percent,
  Activity,
  FileText,
  Shield,
  Loader2,
  AlertCircle
} from "lucide-react";

interface ReservationOverview {
  id: string;
  guestName: string;
  unitName: string;
  startDate: string;
  endDate: string;
  status: string;
  pricePerNightSnapshot?: number;
  totalPrice?: number;
}

interface ReservationMetrics {
  totalReservations: number;
  occupancyRate: number;
  occupiedDays: number;
}

interface BillingMetrics {
  revenueFromSettlements: number;
  unpaidBalance: number;
  paidSettlementsCount: number;
  unpaidSettlementsCount: number;
}

interface ChartDataPoint {
  label: string;
  occupancy: number;
  staticAdr: number;
  aiAdr: number;
  grossRevenue: number;
  utilityCosts: number;
  netProfit: number;
}

interface RankingDataPoint {
  name: string;
  revenue: number;
  bookings: number;
}

export default function DashboardPage() {
  const getInitialDates = () => {
    const now = new Date();
    const start = new Date();
    start.setDate(now.getDate() - 30);
    return {
      start,
      end: now,
    };
  };

  const initialDates = getInitialDates();
  const [startDate, setStartDate] = useState<Date | null>(initialDates.start);
  const [endDate, setEndDate] = useState<Date | null>(initialDates.end);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [userRole, setUserRole] = useState<"ADMIN" | "OWNER" | "GUEST" | "">("");

  const hasFetched = useRef(false);
  const dateToRef = useRef<DatePicker | null>(null);

  const [resMetrics, setResMetrics] = useState<ReservationMetrics>({
    totalReservations: 0,
    occupancyRate: 0.0,
    occupiedDays: 0,
  });

  const [billMetrics, setBillMetrics] = useState<BillingMetrics>({
    revenueFromSettlements: 0,
    unpaidBalance: 0,
    paidSettlementsCount: 0,
    unpaidSettlementsCount: 0,
  });

  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [rankingData, setRankingData] = useState<RankingDataPoint[]>([]);
  const [totalUnitsCount, setTotalUnitsCount] = useState<number>(0);

  const totalReservations = resMetrics?.totalReservations ?? 0;
  const occupancyRate = resMetrics?.occupancyRate ?? 0;
  const occupiedDays = resMetrics?.occupiedDays ?? 0;

  const revenueFromSettlements = billMetrics?.revenueFromSettlements ?? 0;
  const unpaidBalance = billMetrics?.unpaidBalance ?? 0;
  const paidSettlementsCount = billMetrics?.paidSettlementsCount ?? 0;
  const unpaidSettlementsCount = billMetrics?.unpaidSettlementsCount ?? 0;


  const fetchAllReservations = async (isAdmin: boolean): Promise<ReservationOverview[]> => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return [];
      const res = await fetch(`${GATEWAY_BASE_URL}/api/v1/admin/reservations`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      if (!res.ok) {
        if (isAdmin) {
          console.error(`Admin failed to fetch reservations: ${res.status}`);
        } else {
          console.warn(`Owner/non-admin reservation loading bypassed or failed with status ${res.status}. Returning empty list.`);
        }
        return [];
      }
      return await res.json();
    } catch (err) {
      console.error("Error in fetchAllReservations:", err);
      return [];
    }
  };

  const fetchTotalUnits = async (isAdminRole: boolean): Promise<Unit[]> => {
    try {
      if (isAdminRole) {
        const properties = await getProperties();
        if (!properties || !Array.isArray(properties)) return [];
        const unitsPromises = properties.map(async (p) => {
          try {
            const resUnits = await getUnits(p.id);
            return Array.isArray(resUnits) ? (resUnits as Unit[]) : [];
          } catch {
            return [];
          }
        });
        const unitsLists = await Promise.all(unitsPromises);
        return unitsLists.flat();
      } else {
        const properties = await getMyProperties();
        if (!properties || !Array.isArray(properties)) return [];
        const unitsPromises = properties.map(async (p) => {
          try {
            const resUnits = await getPropertyUnits(p.id);
            return Array.isArray(resUnits) ? (resUnits as Unit[]) : [];
          } catch {
            return [];
          }
        });
        const unitsLists = await Promise.all(unitsPromises);
        return unitsLists.flat();
      }
    } catch (err) {
      console.error("Error fetching units count:", err);
      return [];
    }
  };

  const fetchData = useCallback(async (start: Date | null, end: Date | null) => {
    if (!start || !end) return;
    setLoading(true);
    setError(null);
    try {
      const startStr = format(start, "yyyy-MM-dd");
      const endStr = format(end, "yyyy-MM-dd");

      const token = localStorage.getItem("token");
      const roles = getUserRoles(token);
      const isAdminRole = roles.includes("ROLE_ADMIN");
      const isOwnerRole = roles.includes("ROLE_OWNER");
      
      let detectedRole: "ADMIN" | "OWNER" | "GUEST" = "GUEST";
      if (isAdminRole) detectedRole = "ADMIN";
      else if (isOwnerRole) detectedRole = "OWNER";
      setUserRole(detectedRole);

      const [resData, billData, rawReservations, unitsList] = await Promise.all([
        getDashboardReservationMetrics(startStr, endStr),
        getDashboardBillingMetrics(startStr, endStr),
        fetchAllReservations(isAdminRole),
        fetchTotalUnits(isAdminRole)
      ]);

      setResMetrics(resData);
      setBillMetrics(billData);

      const unitsCount = Array.isArray(unitsList) ? unitsList.filter(Boolean).length : 0;
      setTotalUnitsCount(unitsCount);

      const unitsMap = new Map<string, Unit>(
        Array.isArray(unitsList) ? unitsList.filter(u => u && u.name).map(u => [u.name, u]) : []
      );

      const avgBasePrice = unitsCount > 0
        ? unitsList.filter(Boolean).reduce((sum, u) => sum + (u.pricePerNight || 0), 0) / unitsCount
        : 180;

      const rangeStartStr = format(start, "yyyy-MM-dd");
      const rangeEndStr = format(end, "yyyy-MM-dd");
      
      const activeOverlapping = Array.isArray(rawReservations)
        ? rawReservations.filter((res) => {
            return (
              res &&
              res.status !== "CANCELLED" &&
              res.startDate &&
              res.endDate &&
              res.startDate <= rangeEndStr &&
              res.endDate >= rangeStartStr
            );
          })
        : [];

      const settlementsPromises = activeOverlapping.map(async (res) => {
        try {
          const settlement = await getSettlementDetails(res.id);
          return { reservationId: res.id, settlement };
        } catch {
          return { reservationId: res.id, settlement: null };
        }
      });
      const settlementsResults = await Promise.all(settlementsPromises);
      const settlementsMap = new Map(settlementsResults.map(s => [s.reservationId, s.settlement]));

      const dateList: Date[] = [];
      const curr = new Date(start);
      curr.setHours(0, 0, 0, 0);
      const endLimit = new Date(end);
      endLimit.setHours(0, 0, 0, 0);

      while (curr <= endLimit) {
        dateList.push(new Date(curr));
        curr.setDate(curr.getDate() + 1);
      }

      const dailyOccupancyMap = new Map<string, number>();
      const dailyGrossRevenueMap = new Map<string, number>();
      const dailyUtilityCostsMap = new Map<string, number>();

      dateList.forEach(date => {
        const dStr = format(date, "yyyy-MM-dd");
        dailyOccupancyMap.set(dStr, 0);
        dailyGrossRevenueMap.set(dStr, 0);
        dailyUtilityCostsMap.set(dStr, 0);
      });

      dateList.forEach(date => {
        const dStr = format(date, "yyyy-MM-dd");
        const occupiedUnits = new Set<string>();
        
        activeOverlapping.forEach((res) => {
          if (res.startDate <= dStr && dStr < res.endDate) {
            occupiedUnits.add(res.unitName || res.id);
          }
        });

        const rate = unitsCount > 0 ? (occupiedUnits.size / unitsCount) * 100 : 0;
        dailyOccupancyMap.set(dStr, rate);
      });

      activeOverlapping.forEach((res) => {
        const settlement = settlementsMap.get(res.id);
        if (settlement) {
          const revenue = settlement.accommodationAmount || settlement.totalAmount || 0;
          const utilities = settlement.utilitiesAmount || 0;

          const rawDate = settlement.paidAt || settlement.issuedAt || res.startDate;
          if (rawDate) {
            try {
              const dateStr = format(new Date(rawDate), "yyyy-MM-dd");
              const revEntry = dailyGrossRevenueMap.get(dateStr);
              if (revEntry !== undefined) dailyGrossRevenueMap.set(dateStr, revEntry + revenue);
              
              const utilEntry = dailyUtilityCostsMap.get(dateStr);
              if (utilEntry !== undefined) dailyUtilityCostsMap.set(dateStr, utilEntry + utilities);
            } catch (e) {
              console.error("Failed to parse settlement date", rawDate, e);
            }
          }
        } else {
          const dateStr = res.startDate;
          if (dateStr) {
            const revEntry = dailyGrossRevenueMap.get(dateStr);
            if (revEntry !== undefined) {
              dailyGrossRevenueMap.set(dateStr, revEntry + (res.totalPrice || 0));
            }
          }
        }
      });

      const dailyPoints = dateList.map(date => {
        const dStr = format(date, "yyyy-MM-dd");
        const occupancy = dailyOccupancyMap.get(dStr) || 0;
        
        let occupiedCount = 0;
        let sumStatic = 0;
        let sumActual = 0;

        activeOverlapping.forEach(res => {
          if (res.startDate <= dStr && dStr < res.endDate) {
            occupiedCount++;
            const uInfo = unitsMap.get(res.unitName);
            sumStatic += uInfo ? (uInfo.pricePerNight || 0) : (res.pricePerNightSnapshot || 0);
            sumActual += res.pricePerNightSnapshot || 0;
          }
        });

        const staticAdr = occupiedCount > 0 ? (sumStatic / occupiedCount) : avgBasePrice;
        const aiAdr = occupiedCount > 0 ? (sumActual / occupiedCount) : avgBasePrice;

        const grossRevenue = dailyGrossRevenueMap.get(dStr) || 0;
        const utilityCosts = dailyUtilityCostsMap.get(dStr) || 0;

        return {
          date,
          dateStr: dStr,
          label: format(date, "MMM dd"),
          occupancy: Math.round(occupancy * 10) / 10,
          staticAdr: Math.round(staticAdr * 100) / 100,
          aiAdr: Math.round(aiAdr * 100) / 100,
          grossRevenue: Math.round(grossRevenue * 100) / 100,
          utilityCosts: Math.round(utilityCosts * 100) / 100,
          netProfit: Math.round((grossRevenue - utilityCosts) * 100) / 100
        };
      });

      const diffDays = dailyPoints.length;
      let grouped: ChartDataPoint[] = [];

      if (diffDays <= 31) {
        grouped = dailyPoints.map(p => ({
          label: p.label,
          occupancy: p.occupancy,
          staticAdr: p.staticAdr,
          aiAdr: p.aiAdr,
          grossRevenue: p.grossRevenue,
          utilityCosts: p.utilityCosts,
          netProfit: p.netProfit
        }));
      } else if (diffDays <= 120) {
        const weeks: Record<string, typeof dailyPoints> = {};
        dailyPoints.forEach(p => {
          const dayOfWeek = p.date.getDay();
          const diff = p.date.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
          const weekStart = new Date(p.date);
          weekStart.setDate(diff);
          const weekKey = format(weekStart, "yyyy-MM-dd");
          if (!weeks[weekKey]) {
            weeks[weekKey] = [];
          }
          weeks[weekKey].push(p);
        });

        grouped = Object.keys(weeks).sort().map(weekKey => {
          const points = weeks[weekKey];
          const avgOccupancy = points.reduce((sum, p) => sum + p.occupancy, 0) / points.length;
          const avgStaticAdr = points.reduce((sum, p) => sum + p.staticAdr, 0) / points.length;
          const avgAiAdr = points.reduce((sum, p) => sum + p.aiAdr, 0) / points.length;
          const sumGross = points.reduce((sum, p) => sum + p.grossRevenue, 0);
          const sumUtil = points.reduce((sum, p) => sum + p.utilityCosts, 0);
          const weekStart = new Date(weekKey);
          return {
            label: `Wk of ${format(weekStart, "MMM dd")}`,
            occupancy: Math.round(avgOccupancy * 10) / 10,
            staticAdr: Math.round(avgStaticAdr * 100) / 100,
            aiAdr: Math.round(avgAiAdr * 100) / 100,
            grossRevenue: Math.round(sumGross * 100) / 100,
            utilityCosts: Math.round(sumUtil * 100) / 100,
            netProfit: Math.round((sumGross - sumUtil) * 100) / 100
          };
        });
      } else {
        const months: Record<string, typeof dailyPoints> = {};
        dailyPoints.forEach(p => {
          const monthKey = format(p.date, "yyyy-MM");
          if (!months[monthKey]) {
            months[monthKey] = [];
          }
          months[monthKey].push(p);
        });

        grouped = Object.keys(months).sort().map(monthKey => {
          const points = months[monthKey];
          const avgOccupancy = points.reduce((sum, p) => sum + p.occupancy, 0) / points.length;
          const avgStaticAdr = points.reduce((sum, p) => sum + p.staticAdr, 0) / points.length;
          const avgAiAdr = points.reduce((sum, p) => sum + p.aiAdr, 0) / points.length;
          const sumGross = points.reduce((sum, p) => sum + p.grossRevenue, 0);
          const sumUtil = points.reduce((sum, p) => sum + p.utilityCosts, 0);
          const parsedDate = new Date(monthKey + "-02");
          return {
            label: format(parsedDate, "MMM yyyy"),
            occupancy: Math.round(avgOccupancy * 10) / 10,
            staticAdr: Math.round(avgStaticAdr * 100) / 100,
            aiAdr: Math.round(avgAiAdr * 100) / 100,
            grossRevenue: Math.round(sumGross * 100) / 100,
            utilityCosts: Math.round(sumUtil * 100) / 100,
            netProfit: Math.round((sumGross - sumUtil) * 100) / 100
          };
        });
      }

      setChartData(grouped);

      const rankingMap = new Map<string, { revenue: number; bookings: number }>();
      activeOverlapping.forEach(res => {
        const uName = res.unitName || "Unknown Unit";
        const settlement = settlementsMap.get(res.id);
        const rev = settlement ? (settlement.totalAmount || 0) : (res.totalPrice || 0);
        
        const entry = rankingMap.get(uName) || { revenue: 0, bookings: 0 };
        rankingMap.set(uName, {
          revenue: entry.revenue + rev,
          bookings: entry.bookings + 1
        });
      });

      const rankingDataPoints = Array.from(rankingMap.entries())
        .map(([name, val]) => ({
          name,
          revenue: Math.round(val.revenue * 100) / 100,
          bookings: val.bookings
        }))
        .sort((a, b) => b.revenue - a.revenue)
        .slice(0, 8);

      setRankingData(rankingDataPoints);
    } catch (err: unknown) {
      console.error(err);
      const errMsg = err instanceof Error ? err.message : "Failed to retrieve dashboard metrics.";
      setError(errMsg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (hasFetched.current) return;
    hasFetched.current = true;

    const initFetch = async () => {
      await Promise.resolve();
      fetchData(startDate, endDate);
    };
    initFetch();
  }, [startDate, endDate, fetchData]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchData(startDate, endDate);
  };

  const handleDownloadReport = () => {
    if (!startDate || !endDate) return;
    const startStr = format(startDate, "yyyy-MM-dd");
    const endStr = format(endDate, "yyyy-MM-dd");

    const csvRows = [
      ["KWATERA PROPERTY MANAGEMENT - SYSTEM REPORT"],
      [`Generated on: ${new Date().toLocaleString()}`],
      [`Period: ${startStr} to ${endStr}`],
      [`Role Scoping: ${userRole}`],
      [],
      ["RESERVATION METRICS"],
      ["Metric", "Value"],
      ["Total Reservations", String(totalReservations)],
      ["Occupancy Rate", `${occupancyRate}%`],
      ["Occupied Days", String(occupiedDays)],
      ["Total Scoped Properties/Units", String(totalUnitsCount)],
      [],
      ["BILLING METRICS"],
      ["Metric", "Value (PLN)"],
      ["Total Collected Revenue", String(revenueFromSettlements)],
      ["Unpaid Receivables Balance", String(unpaidBalance)],
      ["Paid Invoices Count", String(paidSettlementsCount)],
      ["Unpaid Invoices Count", String(unpaidSettlementsCount)]
    ];

    const csvContent = csvRows
      .map(row => row.map(val => val.includes(",") ? `"${val}"` : val).join(","))
      .join("\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `kwatera-${userRole.toLowerCase()}-report-${startStr}-to-${endStr}.csv`);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const totalSettlements = paidSettlementsCount + unpaidSettlementsCount;
  const paidPct = totalSettlements > 0 ? (paidSettlementsCount / totalSettlements) * 100 : 0;



  return (
    <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-8">
      {/* Header section */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-[#DACDCA] pb-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">Dashboard</h1>
            {userRole && (
              <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider ${
                userRole === "ADMIN" 
                  ? "bg-red-50 text-red-800 border-red-200" 
                  : "bg-indigo-50 text-indigo-800 border-indigo-200"
              }`}>
                <Shield className="w-3.5 h-3.5" />
                {userRole === "ADMIN" ? "Global Administrator" : "Property Owner"}
              </span>
            )}
          </div>
          <p className="text-sm text-[#7A7A7A]">
            {userRole === "ADMIN" 
              ? "Global performance metrics, system-wide occupancy, and cashflow volume."
              : "Scoped owner stats, property occupancy trends, and unit revenue collection."}
          </p>
        </div>

        <form onSubmit={handleFilterSubmit} className="flex flex-wrap items-center gap-3 bg-white p-3 rounded-xl shadow-sm border border-[#DACDCA]">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">From</span>
            <div className="flex items-center bg-[#F7F7F7] border border-[#DACDCA] rounded-lg px-3 py-1.5 shadow-sm focus-within:ring-1 focus-within:ring-[#42211D]">
              <SharedDatePicker
                selected={startDate}
                onChange={(date) => {
                  setStartDate(date);
                  if (date) {
                    setTimeout(() => {
                      dateToRef.current?.setOpen(true);
                    }, 100);
                  }
                }}
                selectsStart
                startDate={startDate}
                endDate={endDate}
                placeholderText="Start"
                className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
              />
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">To</span>
            <div className="flex items-center bg-[#F7F7F7] border border-[#DACDCA] rounded-lg px-3 py-1.5 shadow-sm focus-within:ring-1 focus-within:ring-[#42211D]">
              <SharedDatePicker
                datepickerRef={dateToRef}
                selected={endDate}
                onChange={(date) => setEndDate(date)}
                selectsEnd
                startDate={startDate}
                endDate={endDate}
                minDate={startDate}
                placeholderText="End"
                className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
              />
            </div>
          </div>
          <button
            type="submit"
            className="px-5 py-2 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm cursor-pointer"
          >
            Filter
          </button>
          <button
            type="button"
            onClick={handleDownloadReport}
            className="px-4 py-2 text-sm font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm cursor-pointer flex items-center gap-2"
          >
            <FileText className="w-4 h-4" />
            Export CSV
          </button>
        </form>
      </div>

      {error && (
        <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span className="text-sm font-medium">{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-8">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm animate-pulse space-y-4">
                <div className="h-4 w-1/3 bg-[#F7F7F7] rounded"></div>
                <div className="h-8 w-2/3 bg-[#F7F7F7] rounded"></div>
                <div className="h-3 w-1/2 bg-[#F7F7F7] rounded"></div>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <div className="bg-white p-8 rounded-xl border border-[#DACDCA] shadow-sm animate-pulse h-96 flex items-center justify-center">
              <Loader2 className="w-8 h-8 text-[#42211D] animate-spin" />
            </div>
            <div className="bg-white p-8 rounded-xl border border-[#DACDCA] shadow-sm animate-pulse h-96 flex items-center justify-center">
              <Loader2 className="w-8 h-8 text-[#42211D] animate-spin" />
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-8">
          {/* 1. Statistic tiles (Must NOT delete) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-[#42211D]/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    Reservations
                  </span>
                  <span className="p-2 bg-[#42211D]/10 text-[#42211D] rounded-xl">
                    <Calendar className="w-5 h-5" />
                  </span>
                </div>
                <div>
                  <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                    {totalReservations}
                  </h2>
                  <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                    Active bookings in selected range
                  </p>
                </div>
              </div>
            </div>

            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    Occupancy Rate
                  </span>
                  <span className="p-2 bg-emerald-100 text-emerald-700 rounded-xl">
                    <Percent className="w-5 h-5" />
                  </span>
                </div>
                <div>
                  <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight flex items-baseline gap-1">
                    {occupancyRate}%
                  </h2>
                  <div className="w-full bg-[#F7F7F7] border border-[#DACDCA] h-2 rounded-full mt-3 overflow-hidden">
                    <div
                      className="bg-emerald-600 h-full rounded-full transition-all duration-1000"
                      style={{ width: `${occupancyRate}%` }}
                    ></div>
                  </div>
                  <p className="text-[10px] text-[#7A7A7A] mt-2 font-medium">
                    {occupiedDays} total nights ({totalUnitsCount} units monitored)
                  </p>
                </div>
              </div>
            </div>

            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    Collected Revenue
                  </span>
                  <span className="p-2 bg-blue-100 text-blue-700 rounded-xl">
                    <DollarSign className="w-5 h-5" />
                  </span>
                </div>
                <div>
                  <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                    {revenueFromSettlements.toLocaleString("pl-PL", { style: "currency", currency: "PLN" })}
                  </h2>
                  <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                    Paid settlements and deposits
                  </p>
                </div>
              </div>
            </div>

            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    Outstanding Receivables
                  </span>
                  <span className="p-2 bg-amber-100 text-amber-700 rounded-xl">
                    <Activity className="w-5 h-5" />
                  </span>
                </div>
                <div>
                  <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                    {unpaidBalance.toLocaleString("pl-PL", { style: "currency", currency: "PLN" })}
                  </h2>
                  <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                    Pending invoice settlements
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* 2. Responsive CSS Grid for Core Analytics Charts (recharts) */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            
            {/* Chart 1: Occupancy Trend (Area Chart) */}
            <div className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                  Occupancy Trend
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                  Shows how occupancy percentages fluctuate over the selected range.
                </p>
              </div>
              <div className="h-80 w-full">
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                      <defs>
                        <linearGradient id="occupancyGrad" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#10B981" stopOpacity={0.2} />
                          <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F1F1" vertical={false} />
                      <XAxis dataKey="label" stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} dy={8} />
                      <YAxis stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                      <Tooltip content={<CustomOccupancyTooltip />} wrapperStyle={{ backgroundColor: 'transparent', border: 'none', outline: 'none' }} />
                      <Area type="monotone" dataKey="occupancy" stroke="#10B981" strokeWidth={2.5} fillOpacity={1} fill="url(#occupancyGrad)" />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                    No occupancy trend data available
                  </div>
                )}
              </div>
            </div>

            {/* Chart 2: Base vs. Sold Price Analysis (Line Chart) */}
            <div className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                  Base vs. Sold Price Analysis
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                  Comparison between the base property price and the actual price sold (ADR).
                </p>
              </div>
              <div className="h-80 w-full">
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F1F1" vertical={false} />
                      <XAxis dataKey="label" stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} dy={8} />
                      <YAxis stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} tickFormatter={(v) => `zł${v}`} />
                      <Tooltip content={<CustomPricingTooltip />} wrapperStyle={{ backgroundColor: 'transparent', border: 'none', outline: 'none' }} />
                      <Legend verticalAlign="top" height={36} iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 11, fontWeight: "bold" }} />
                      <Line type="monotone" dataKey="staticAdr" name="Static Base Price" stroke="#9CA3AF" strokeWidth={2} strokeDasharray="5 5" dot={false} activeDot={{ r: 4 }} />
                      <Line type="monotone" dataKey="aiAdr" name="Actual Price (ADR)" stroke="#6366F1" strokeWidth={2.5} dot={{ r: 3, strokeWidth: 1 }} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                    No dynamic pricing data available
                  </div>
                )}
              </div>
            </div>

            {/* Chart 3: Net Profit & Utility Expenses (Composed Chart) */}
            <div className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                  Net Profit & Utility Expenses
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                  Gross revenue mapped against media and utility expenses.
                </p>
              </div>
              <div className="h-80 w-full">
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F1F1" vertical={false} />
                      <XAxis dataKey="label" stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} dy={8} />
                      <YAxis stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} tickFormatter={(v) => `zł${v}`} />
                      <Tooltip content={<CustomProfitTooltip />} wrapperStyle={{ backgroundColor: 'transparent', border: 'none', outline: 'none' }} />
                      <Legend verticalAlign="top" height={36} iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 11, fontWeight: "bold" }} />
                      <Bar dataKey="grossRevenue" name="Gross Revenue" fill="#3B82F6" radius={[4, 4, 0, 0]} barSize={20} />
                      <Bar dataKey="utilityCosts" name="Utility Expenses" fill="#EF4444" radius={[4, 4, 0, 0]} barSize={20} />
                      <Line type="monotone" dataKey="netProfit" name="Net Profit" stroke="#10B981" strokeWidth={3} dot={{ r: 4 }} />
                    </ComposedChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                    No financial transaction history in this range
                  </div>
                )}
              </div>
            </div>

            {/* Chart 4: Unit Performance Ranking (Horizontal Bar Chart) */}
            <div className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                  Unit Profitability Ranking
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                  Top performing accommodations sorted by gross rental revenue.
                </p>
              </div>
              <div className="h-80 w-full">
                {rankingData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      layout="vertical"
                      data={rankingData}
                      margin={{ top: 10, right: 10, left: 15, bottom: 5 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F1F1" horizontal={false} />
                      <XAxis type="number" stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} tickFormatter={(v) => `zł${v}`} />
                      <YAxis type="category" dataKey="name" stroke="#7A7A7A" fontSize={10} tickLine={false} axisLine={false} width={80} />
                      <Tooltip content={<CustomRankingTooltip />} wrapperStyle={{ backgroundColor: 'transparent', border: 'none', outline: 'none' }} />
                      <Bar dataKey="revenue" name="Total Revenue" fill="#42211D" radius={[0, 4, 4, 0]} barSize={14} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                    No active rentals or units resolved in this range
                  </div>
                )}
              </div>
            </div>

          </div>

          {/* Bottom Summaries */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 border-t border-[#DACDCA] pt-8">
            <div className="bg-white p-6 sm:p-8 rounded-xl border border-[#DACDCA] shadow-sm flex flex-col justify-between h-96">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                  Settlements Payment Ratio
                </h3>
                <p className="text-xs text-[#7A7A7A] mt-1">
                  Proportion of paid versus unpaid settlements within date range.
                </p>
              </div>

              <div className="flex flex-col sm:flex-row items-center justify-center gap-8 my-auto">
                {totalSettlements > 0 ? (
                  <>
                    <div className="relative w-40 h-40 flex-shrink-0">
                      <svg className="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                        <circle
                          cx="18"
                          cy="18"
                          r="15.915"
                          fill="none"
                          stroke="#F7F7F7"
                          strokeWidth="3.2"
                        />
                        <circle
                          cx="18"
                          cy="18"
                          r="15.915"
                          fill="none"
                          stroke="#42211D"
                          strokeWidth="3.2"
                          strokeDasharray={`${paidPct} ${100 - paidPct}`}
                          strokeDashoffset="0"
                          className="transition-all duration-1000 ease-out"
                        />
                        <circle
                          cx="18"
                          cy="18"
                          r="15.915"
                          fill="none"
                          stroke="#F59E0B"
                          strokeWidth="3.2"
                          strokeDasharray={`${(100 - paidPct)} ${paidPct}`}
                          strokeDashoffset={`-${paidPct}`}
                          className="transition-all duration-1000 ease-out"
                        />
                      </svg>
                      <div className="absolute inset-0 flex flex-col items-center justify-center">
                        <span className="text-2xl font-black text-[#1A1A1A]">{totalSettlements}</span>
                        <span className="text-[10px] uppercase font-bold tracking-widest text-[#7A7A7A]">Total</span>
                      </div>
                    </div>

                    <div className="flex flex-col gap-3">
                      <div className="flex items-center gap-3">
                        <span className="w-3.5 h-3.5 rounded-full bg-[#42211D] flex-shrink-0"></span>
                        <div className="flex flex-col">
                          <span className="text-sm font-semibold text-gray-700">Paid Invoices</span>
                          <span className="text-xs font-medium text-[#7A7A7A]">
                            {paidSettlementsCount} settlements ({Math.round(paidPct)}%)
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="w-3.5 h-3.5 rounded-full bg-amber-500 flex-shrink-0"></span>
                        <div className="flex flex-col">
                          <span className="text-sm font-semibold text-gray-700">Unpaid/Issued</span>
                          <span className="text-xs font-medium text-[#7A7A7A]">
                            {unpaidSettlementsCount} settlements ({Math.round(100 - paidPct)}%)
                          </span>
                        </div>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="text-center py-10 space-y-3">
                    <div className="mx-auto w-16 h-16 text-gray-200 bg-gray-50 rounded-full flex items-center justify-center">
                      <AlertCircle className="w-8 h-8" />
                    </div>
                    <p className="text-sm font-semibold text-[#7A7A7A]">No Settlement Data Available</p>
                  </div>
                )}
              </div>
            </div>

            <div className="bg-white p-6 sm:p-8 rounded-xl border border-[#DACDCA] shadow-sm flex flex-col justify-between h-96">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                  Financial Volume Breakdown
                </h3>
                <p className="text-xs text-[#7A7A7A] mt-1">
                  Collected revenue compared directly to outstanding receivables.
                </p>
              </div>

              <div className="flex flex-col justify-center gap-6 my-auto h-full w-full">
                {revenueFromSettlements > 0 || unpaidBalance > 0 ? (
                  <div className="space-y-6 w-full px-2 pt-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-semibold text-gray-700 flex items-center gap-2">
                          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                          Collected Revenue
                        </span>
                        <span className="font-bold text-gray-900">
                          {revenueFromSettlements.toLocaleString("pl-PL", { style: "currency", currency: "PLN" })}
                        </span>
                      </div>
                      <div className="w-full bg-[#F7F7F7] border border-[#DACDCA] h-6 rounded-lg overflow-hidden relative shadow-inner">
                        <div
                          className="bg-emerald-500 h-full rounded-lg transition-all duration-1000 ease-out shadow-sm"
                          style={{
                            width: `${
                              revenueFromSettlements + unpaidBalance > 0
                                ? (revenueFromSettlements / (revenueFromSettlements + unpaidBalance)) * 100
                                : 0
                            }%`
                          }}
                        ></div>
                      </div>
                    </div>

                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-semibold text-gray-700 flex items-center gap-2">
                          <span className="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
                          Outstanding Receivables
                        </span>
                        <span className="font-bold text-gray-900">
                          {unpaidBalance.toLocaleString("pl-PL", { style: "currency", currency: "PLN" })}
                        </span>
                      </div>
                      <div className="w-full bg-[#F7F7F7] border border-[#DACDCA] h-6 rounded-lg overflow-hidden relative shadow-inner">
                        <div
                          className="bg-amber-500 h-full rounded-lg transition-all duration-1000 ease-out shadow-sm"
                          style={{
                            width: `${
                              revenueFromSettlements + unpaidBalance > 0
                                ? (unpaidBalance / (revenueFromSettlements + unpaidBalance)) * 100
                                : 0
                            }%`
                          }}
                        ></div>
                      </div>
                    </div>

                    <div className="border-t border-gray-100 pt-4 flex justify-between text-xs text-[#7A7A7A] font-bold">
                      <span>Total Settlements Volume:</span>
                      <span>
                        {(revenueFromSettlements + unpaidBalance).toLocaleString("pl-PL", { style: "currency", currency: "PLN" })}
                      </span>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-10 space-y-3">
                    <div className="mx-auto w-16 h-16 text-gray-200 bg-gray-50 rounded-full flex items-center justify-center">
                      <AlertCircle className="w-8 h-8" />
                    </div>
                    <p className="text-sm font-semibold text-[#7A7A7A]">No Revenue Data Available</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: TooltipPayloadEntry[];
  label?: string | number;
}

const CustomOccupancyTooltip = ({ active, payload, label }: CustomTooltipProps) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white p-3.5 rounded-xl shadow-lg border border-gray-100 text-xs font-semibold space-y-1" style={{ backgroundColor: '#ffffff' }}>
        <p className="text-gray-400 font-bold uppercase tracking-wider text-[9px]">{label}</p>
        <p className="text-sm flex items-center gap-1.5 font-bold text-gray-900">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
          Occupancy: <span className="text-emerald-600 text-base">{payload[0].value}%</span>
        </p>
      </div>
    );
  }
  return null;
};

const CustomPricingTooltip = ({ active, payload, label }: CustomTooltipProps) => {
  if (active && payload && payload.length) {
    const staticVal = payload.find(p => p.dataKey === 'staticAdr')?.value ?? 0;
    const aiVal = payload.find(p => p.dataKey === 'aiAdr')?.value ?? 0;
    const diff = Number(aiVal) - Number(staticVal);
    const pct = Number(staticVal) > 0 ? (diff / Number(staticVal)) * 100 : 0;
    return (
      <div className="bg-white p-4 rounded-xl shadow-lg border border-gray-100 text-xs font-semibold space-y-2" style={{ backgroundColor: '#ffffff' }}>
        <p className="text-gray-400 font-bold uppercase tracking-wider text-[9px]">{label}</p>
        <div className="space-y-1.5">
          <p className="flex items-center justify-between gap-4 font-semibold text-gray-600">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-gray-400"></span>
              Base Static Price:
            </span>
            <span>zł{Number(staticVal).toLocaleString("pl-PL", { minimumFractionDigits: 2 })}</span>
          </p>
          <p className="flex items-center justify-between gap-4 font-semibold text-indigo-600">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-indigo-500"></span>
              Actual Sold Price (ADR):
            </span>
            <span>zł{Number(aiVal).toLocaleString("pl-PL", { minimumFractionDigits: 2 })}</span>
          </p>
          <div className="border-t border-gray-100 pt-1.5 mt-1.5 flex items-center justify-between gap-4 font-bold text-emerald-600">
            <span>Pricing Margin:</span>
            <span>+zł{diff.toLocaleString("pl-PL", { minimumFractionDigits: 2 })} ({pct.toFixed(1)}%)</span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

const CustomProfitTooltip = ({ active, payload, label }: CustomTooltipProps) => {
  if (active && payload && payload.length) {
    const gross = payload.find(p => p.dataKey === 'grossRevenue')?.value ?? 0;
    const utilities = payload.find(p => p.dataKey === 'utilityCosts')?.value ?? 0;
    const net = Number(gross) - Number(utilities);
    return (
      <div className="bg-white p-4 rounded-xl shadow-lg border border-gray-100 text-xs font-semibold space-y-2" style={{ backgroundColor: '#ffffff' }}>
        <p className="text-gray-400 font-bold uppercase tracking-wider text-[9px]">{label}</p>
        <div className="space-y-1.5">
          <p className="flex items-center justify-between gap-4 font-semibold text-blue-600">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
              Gross Revenue:
            </span>
            <span>zł{Number(gross).toLocaleString("pl-PL")}</span>
          </p>
          <p className="flex items-center justify-between gap-4 font-semibold text-red-600">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
              Utility Costs:
            </span>
            <span>zł{Number(utilities).toLocaleString("pl-PL")}</span>
          </p>
          <div className="border-t border-gray-100 pt-1.5 mt-1.5 flex items-center justify-between gap-4 font-bold text-emerald-600">
            <span>Net Profit:</span>
            <span>zł{net.toLocaleString("pl-PL")}</span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

const CustomRankingTooltip = ({ active, payload }: CustomTooltipProps) => {
  if (active && payload && payload.length) {
    const revenue = payload.find(p => p.dataKey === 'revenue')?.value ?? 0;
    const itemPayload = payload[0]?.payload || {};
    const uName = itemPayload.name || "Unknown Unit";
    const bookings = itemPayload.bookings || 0;
    return (
      <div className="bg-white p-3.5 rounded-xl shadow-lg border border-gray-100 text-xs font-semibold space-y-1.5" style={{ backgroundColor: '#ffffff' }}>
        <p className="text-gray-900 font-extrabold">{uName}</p>
        <p className="text-sm font-bold text-gray-700 flex justify-between gap-4">
          <span>Total Revenue:</span>
          <span className="text-[#42211D]">zł{Number(revenue).toLocaleString("pl-PL")}</span>
        </p>
        <p className="text-xs text-gray-500 flex justify-between gap-4">
          <span>Bookings Count:</span>
          <span>{bookings} stay{bookings === 1 ? "" : "s"}</span>
        </p>
      </div>
    );
  }
  return null;
};
