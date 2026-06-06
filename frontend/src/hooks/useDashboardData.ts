import { useState, useEffect, useRef, useCallback } from "react";
import { format } from "date-fns";
import { getDashboardReservationMetrics, getDashboardBillingMetrics } from "../api/adminApi";
import { getMyProperties } from "../api/ownerPropertyApi";
import { getPropertyUnits } from "../api/ownerUnitApi";
import { getProperties, getUnits } from "../api/propertyApi";
import { getSettlementDetails } from "../api/settlementApi";
import { getUserRoles } from "../utils/jwtUtils";
import { GATEWAY_BASE_URL } from "../api/apiConfig";
import type { Unit } from "../types/property";

export interface ReservationOverview {
  id: string;
  guestName: string;
  unitName: string;
  startDate: string;
  endDate: string;
  status: string;
  pricePerNightSnapshot?: number;
  totalPrice?: number;
}

export interface ReservationMetrics {
  totalReservations: number;
  occupancyRate: number;
  occupiedDays: number;
}

export interface BillingMetrics {
  revenueFromSettlements: number;
  unpaidBalance: number;
  paidSettlementsCount: number;
  unpaidSettlementsCount: number;
}

export interface ChartDataPoint {
  label: string;
  occupancy: number;
  staticAdr: number;
  aiAdr: number;
  grossRevenue: number;
  utilityCosts: number;
  netProfit: number;
}

export interface RankingDataPoint {
  name: string;
  revenue: number;
  bookings: number;
}

const getInitialDates = () => {
  const now = new Date();
  const start = new Date();
  start.setDate(now.getDate() - 30);
  return { start, end: now };
};

export function useDashboardData() {
  const [dates] = useState(() => getInitialDates());
  const [startDate, setStartDate] = useState<Date | null>(dates.start);
  const [endDate, setEndDate] = useState<Date | null>(dates.end);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [userRole, setUserRole] = useState<"ADMIN" | "OWNER" | "GUEST" | "">("");

  const hasFetched = useRef(false);

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
  const [activeReservations, setActiveReservations] = useState<ReservationOverview[]>([]);

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
      setActiveReservations(activeOverlapping);

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

    fetchData(startDate, endDate);
  }, [startDate, endDate, fetchData]);

  const refreshData = useCallback(() => {
    fetchData(startDate, endDate);
  }, [startDate, endDate, fetchData]);

  return {
    startDate,
    setStartDate,
    endDate,
    setEndDate,
    loading,
    error,
    userRole,
    resMetrics,
    billMetrics,
    chartData,
    rankingData,
    totalUnitsCount,
    activeReservations,
    refreshData
  };
}
