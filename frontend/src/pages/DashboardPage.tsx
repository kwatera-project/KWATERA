import { useState, useEffect, useRef, useCallback } from "react";
import { format } from "date-fns";
import { getDashboardReservationMetrics, getDashboardBillingMetrics } from "../api/adminApi";
import SharedDatePicker from "../components/SharedDatePicker";

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

export default function DashboardPage() {
  const getInitialDates = () => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    return {
      start: firstDay,
      end: lastDay,
    };
  };

  const initialDates = getInitialDates();
  const [startDate, setStartDate] = useState<Date | null>(initialDates.start);
  const [endDate, setEndDate] = useState<Date | null>(initialDates.end);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  const fetchData = useCallback(async (start: Date | null, end: Date | null) => {
    if (!start || !end) return;
    setLoading(true);
    setError(null);
    try {
      const startStr = format(start, "yyyy-MM-dd");
      const endStr = format(end, "yyyy-MM-dd");
      const [resData, billData] = await Promise.all([
        getDashboardReservationMetrics(startStr, endStr),
        getDashboardBillingMetrics(startStr, endStr),
      ]);
      setResMetrics(resData);
      setBillMetrics(billData);
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
      ["KWATERA PROPERTY MANAGEMENT - ADMIN/OWNER DASHBOARD REPORT"],
      [`Generated on: ${new Date().toLocaleString()}`],
      [`Period: ${startStr} to ${endStr}`],
      [],
      ["RESERVATION METRICS"],
      ["Metric", "Value"],
      ["Total Reservations", String(resMetrics.totalReservations)],
      ["Occupancy Rate", `${resMetrics.occupancyRate}%`],
      ["Occupied Days", String(resMetrics.occupiedDays)],
      [],
      ["BILLING METRICS"],
      ["Metric", "Value (PLN)"],
      ["Total Revenue (Paid Settlements)", String(billMetrics.revenueFromSettlements)],
      ["Unpaid Balance (Receivables)", String(billMetrics.unpaidBalance)],
      ["Paid Settlements Count", String(billMetrics.paidSettlementsCount)],
      ["Unpaid Settlements Count", String(billMetrics.unpaidSettlementsCount)]
    ];

    const csvContent = csvRows
      .map(row => row.map(val => val.includes(",") ? `"${val}"` : val).join(","))
      .join("\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `kwatera-dashboard-report-${startStr}-to-${endStr}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const totalSettlements = billMetrics.paidSettlementsCount + billMetrics.unpaidSettlementsCount;
  const paidPct = totalSettlements > 0 ? (billMetrics.paidSettlementsCount / totalSettlements) * 100 : 0;

  return (
    <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A]">
      <div className="max-w-7xl mx-auto space-y-8">
        
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-[#DACDCA] pb-6">
          <div>
            <h1 className="text-3xl font-bold text-[#1A1A1A]">Dashboard</h1>
            <p className="text-sm text-[#7A7A7A] mt-1">
              Monitor reservation occupancy, settlements, and revenue summaries.
            </p>
          </div>

          <form onSubmit={handleFilterSubmit} className="flex flex-wrap items-center gap-3 bg-white p-3 rounded-xl shadow-sm border border-[#DACDCA]">
            <div className="flex items-center gap-2">
              <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">From</label>
              <div className="flex items-center bg-[#F7F7F7] border border-[#DACDCA] rounded-lg px-3 py-1.5 shadow-sm focus-within:ring-1 focus-within:ring-[#42211D] gap-2">
                <SharedDatePicker
                  selected={startDate}
                  onChange={(date) => setStartDate(date)}
                  selectsStart
                  startDate={startDate}
                  endDate={endDate}
                  placeholderText="Start"
                  className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">To</label>
              <div className="flex items-center bg-[#F7F7F7] border border-[#DACDCA] rounded-lg px-3 py-1.5 shadow-sm focus-within:ring-1 focus-within:ring-[#42211D] gap-2">
                <SharedDatePicker
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
              className="px-6 py-2 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm cursor-pointer"
            >
              Filter
            </button>
            <button
              type="button"
              onClick={handleDownloadReport}
              className="px-4 py-2 text-sm font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm cursor-pointer flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
              </svg>
              Export CSV
            </button>
          </form>
        </div>

        {error && (
          <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm">
            <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
            </svg>
            <span className="text-sm font-medium">{error}</span>
          </div>
        )}

        {loading ? (
          <>
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
              {[...Array(2)].map((_, i) => (
                <div key={i} className="bg-white p-8 rounded-xl border border-[#DACDCA] shadow-sm animate-pulse h-96"></div>
              ))}
            </div>
          </>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              
              <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-[#42211D]/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                      Reservations
                    </span>
                    <span className="p-2 bg-[#42211D]/10 text-[#42211D] rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                      {resMetrics.totalReservations}
                    </h2>
                    <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                      Active reservations within range
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
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-bold text-[#1A1A1A] tracking-tight flex items-baseline gap-1">
                      {resMetrics.occupancyRate}%
                    </h2>
                    <div className="w-full bg-[#F7F7F7] border border-[#DACDCA] h-2 rounded-full mt-3 overflow-hidden">
                      <div 
                        className="bg-emerald-600 h-full rounded-full transition-all duration-1000" 
                        style={{ width: `${resMetrics.occupancyRate}%` }}
                      ></div>
                    </div>
                    <p className="text-[10px] text-[#7A7A7A] mt-2 font-medium">
                      {resMetrics.occupiedDays} total occupied nights
                    </p>
                  </div>
                </div>
              </div>

              <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                      Total Revenue
                    </span>
                    <span className="p-2 bg-blue-100 text-blue-700 rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                      {billMetrics.revenueFromSettlements.toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                    </h2>
                    <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                      Received settlements payments
                    </p>
                  </div>
                </div>
              </div>

              <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                      Unpaid Balance
                    </span>
                    <span className="p-2 bg-amber-100 text-amber-700 rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                      {billMetrics.unpaidBalance.toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                    </h2>
                    <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                      Outstanding invoice receivables
                    </p>
                  </div>
                </div>
              </div>

            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              
              <div className="bg-white p-6 sm:p-8 rounded-xl border border-[#DACDCA] shadow-sm flex flex-col justify-between h-96">
                <div>
                  <h3 className="text-lg font-bold text-[#1A1A1A]">
                    Settlements Payment Status
                  </h3>
                  <p className="text-xs text-[#7A7A7A] mt-1">
                    Proportion of paid versus unpaid settlements within date range.
                  </p>
                </div>

                <div className="flex flex-col sm:flex-row items-center justify-center gap-8 my-auto">
                  {totalSettlements > 0 ? (
                    <>
                      <div className="relative w-40 h-40">
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
                              {billMetrics.paidSettlementsCount} settlements ({Math.round(paidPct)}%)
                            </span>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="w-3.5 h-3.5 rounded-full bg-amber-500 flex-shrink-0"></span>
                          <div className="flex flex-col">
                            <span className="text-sm font-semibold text-gray-700">Unpaid/Issued</span>
                            <span className="text-xs font-medium text-[#7A7A7A]">
                              {billMetrics.unpaidSettlementsCount} settlements ({Math.round(100 - paidPct)}%)
                            </span>
                          </div>
                        </div>
                      </div>
                    </>
                  ) : (
                    <div className="text-center py-10 space-y-3">
                      <div className="mx-auto w-16 h-16 text-gray-200 bg-gray-50 rounded-full flex items-center justify-center">
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                        </svg>
                      </div>
                      <p className="text-sm font-semibold text-[#7A7A7A]">No Settlement Data Available</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="bg-white p-6 sm:p-8 rounded-xl border border-[#DACDCA] shadow-sm flex flex-col justify-between h-96">
                <div>
                  <h3 className="text-lg font-bold text-[#1A1A1A]">
                    Financial Volume Summary
                  </h3>
                  <p className="text-xs text-[#7A7A7A] mt-1">
                    Comparison between collected revenue and outstanding receivables.
                  </p>
                </div>

                <div className="flex flex-col justify-center gap-6 my-auto h-full w-full">
                  {billMetrics.revenueFromSettlements > 0 || billMetrics.unpaidBalance > 0 ? (
                    <div className="space-y-6 w-full px-2 pt-6">
                      
                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-sm">
                          <span className="font-semibold text-gray-700 flex items-center gap-2">
                            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                            Collected Revenue
                          </span>
                          <span className="font-bold text-gray-900">
                            {billMetrics.revenueFromSettlements.toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                          </span>
                        </div>
                        <div className="w-full bg-[#F7F7F7] border border-[#DACDCA] h-6 rounded-lg overflow-hidden relative shadow-inner">
                          <div 
                            className="bg-emerald-500 h-full rounded-lg transition-all duration-1000 ease-out shadow-sm"
                            style={{ 
                              width: `${
                                billMetrics.revenueFromSettlements + billMetrics.unpaidBalance > 0
                                  ? (billMetrics.revenueFromSettlements / (billMetrics.revenueFromSettlements + billMetrics.unpaidBalance)) * 100
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
                            {billMetrics.unpaidBalance.toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                          </span>
                        </div>
                        <div className="w-full bg-[#F7F7F7] border border-[#DACDCA] h-6 rounded-lg overflow-hidden relative shadow-inner">
                          <div 
                            className="bg-amber-500 h-full rounded-lg transition-all duration-1000 ease-out shadow-sm"
                            style={{ 
                              width: `${
                                billMetrics.revenueFromSettlements + billMetrics.unpaidBalance > 0
                                  ? (billMetrics.unpaidBalance / (billMetrics.revenueFromSettlements + billMetrics.unpaidBalance)) * 100
                                  : 0
                              }%` 
                            }}
                          ></div>
                        </div>
                      </div>

                      <div className="border-t border-gray-100 pt-4 flex justify-between text-xs text-[#7A7A7A] font-bold">
                        <span>Total Settlements Volume:</span>
                        <span>
                          {(billMetrics.revenueFromSettlements + billMetrics.unpaidBalance).toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                        </span>
                      </div>

                    </div>
                  ) : (
                    <div className="text-center py-10 space-y-3">
                      <div className="mx-auto w-16 h-16 text-gray-200 bg-gray-50 rounded-full flex items-center justify-center">
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                        </svg>
                      </div>
                      <p className="text-sm font-semibold text-[#7A7A7A]">No Revenue Data Available</p>
                    </div>
                  )}
                </div>
              </div>

            </div>
          </>
        )}
      </div>
    </div>
  );
}
