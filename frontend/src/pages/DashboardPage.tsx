import { useState, useEffect } from "react";
import { getDashboardReservationMetrics, getDashboardBillingMetrics } from "../api/adminApi";

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

    const formatDate = (date: Date) => {
      const yyyy = date.getFullYear();
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd}`;
    };

    return {
      start: formatDate(firstDay),
      end: formatDate(lastDay),
    };
  };

  const initialDates = getInitialDates();
  const [startDate, setStartDate] = useState(initialDates.start);
  const [endDate, setEndDate] = useState(initialDates.end);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  const fetchData = async (start: string, end: string) => {
    setLoading(true);
    setError(null);
    try {
      const [resData, billData] = await Promise.all([
        getDashboardReservationMetrics(start, end),
        getDashboardBillingMetrics(start, end),
      ]);
      setResMetrics(resData);
      setBillMetrics(billData);
    } catch (err: any) {
      console.error(err);
      setError(err?.message || "Failed to retrieve dashboard metrics.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData(startDate, endDate);
  }, []);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchData(startDate, endDate);
  };

  const totalSettlements = billMetrics.paidSettlementsCount + billMetrics.unpaidSettlementsCount;
  const paidPct = totalSettlements > 0 ? (billMetrics.paidSettlementsCount / totalSettlements) * 100 : 0;

  return (
    <div className="min-h-screen bg-[#FDFBFB] p-6 lg:p-10 font-sans text-gray-800">
      <div className="max-w-7xl mx-auto space-y-8">
        
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-[#DACDCA]/30 pb-6">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-[rgb(var(--color-burgundy))]">
              Dashboard
            </h1>
            <p className="text-gray-500 mt-1 text-sm md:text-base">
              Monitor reservation occupancy, settlements, and revenue summaries.
            </p>
          </div>

          <form onSubmit={handleFilterSubmit} className="flex flex-wrap items-center gap-3 bg-white p-3 rounded-2xl shadow-sm border border-[#DACDCA]/40">
            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">From</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="px-3 py-1.5 text-sm rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[rgb(var(--color-burgundy))] focus:border-transparent transition"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">To</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="px-3 py-1.5 text-sm rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[rgb(var(--color-burgundy))] focus:border-transparent transition"
              />
            </div>
            <button
              type="submit"
              className="px-4 py-1.5 text-sm font-medium text-white bg-[rgb(var(--color-burgundy))] hover:bg-[rgb(var(--color-burgundy-hover))] rounded-lg transition-all duration-200 shadow-sm cursor-pointer"
            >
              Filter
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
                <div key={i} className="bg-white p-6 rounded-3xl border border-[#DACDCA]/20 shadow-sm animate-pulse space-y-4">
                  <div className="h-4 w-1/3 bg-gray-200 rounded"></div>
                  <div className="h-8 w-2/3 bg-gray-200 rounded"></div>
                  <div className="h-3 w-1/2 bg-gray-200 rounded"></div>
                </div>
              ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {[...Array(2)].map((_, i) => (
                <div key={i} className="bg-white p-8 rounded-3xl border border-[#DACDCA]/20 shadow-sm animate-pulse h-96"></div>
              ))}
            </div>
          </>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              
              <div className="group bg-white p-6 rounded-3xl border border-[#DACDCA]/30 shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-[rgb(var(--color-burgundy))]/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                      Reservations
                    </span>
                    <span className="p-2 bg-[rgb(var(--color-burgundy))]/10 text-[rgb(var(--color-burgundy))] rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">
                      {resMetrics.totalReservations}
                    </h2>
                    <p className="text-xs text-gray-400 mt-2 font-medium">
                      Active reservations within range
                    </p>
                  </div>
                </div>
              </div>

              <div className="group bg-white p-6 rounded-3xl border border-[#DACDCA]/30 shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                      Occupancy Rate
                    </span>
                    <span className="p-2 bg-emerald-100 text-emerald-700 rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight flex items-baseline gap-1">
                      {resMetrics.occupancyRate}%
                    </h2>
                    <div className="w-full bg-gray-100 h-1.5 rounded-full mt-3 overflow-hidden">
                      <div 
                        className="bg-emerald-500 h-full rounded-full transition-all duration-1000" 
                        style={{ width: `${resMetrics.occupancyRate}%` }}
                      ></div>
                    </div>
                    <p className="text-[10px] text-gray-400 mt-2 font-medium">
                      {resMetrics.occupiedDays} total occupied nights
                    </p>
                  </div>
                </div>
              </div>

              <div className="group bg-white p-6 rounded-3xl border border-[#DACDCA]/30 shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                      Total Revenue
                    </span>
                    <span className="p-2 bg-blue-100 text-blue-700 rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">
                      {billMetrics.revenueFromSettlements.toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                    </h2>
                    <p className="text-xs text-gray-400 mt-2 font-medium">
                      Received settlements payments
                    </p>
                  </div>
                </div>
              </div>

              <div className="group bg-white p-6 rounded-3xl border border-[#DACDCA]/30 shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                <div className="flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                      Unpaid Balance
                    </span>
                    <span className="p-2 bg-amber-100 text-amber-700 rounded-xl">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                      </svg>
                    </span>
                  </div>
                  <div>
                    <h2 className="text-3xl font-black text-gray-900 tracking-tight">
                      {billMetrics.unpaidBalance.toLocaleString('pl-PL', { style: 'currency', currency: 'PLN' })}
                    </h2>
                    <p className="text-xs text-gray-400 mt-2 font-medium">
                      Outstanding invoice receivables
                    </p>
                  </div>
                </div>
              </div>

            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              
              <div className="bg-white p-6 sm:p-8 rounded-3xl border border-[#DACDCA]/30 shadow-sm flex flex-col justify-between h-96">
                <div>
                  <h3 className="text-lg font-bold text-[rgb(var(--color-burgundy))]">
                    Settlements Payment Status
                  </h3>
                  <p className="text-xs text-gray-400 mt-1">
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
                            stroke="#F3F4F6"
                            strokeWidth="3.2"
                          />
                          <circle
                            cx="18"
                            cy="18"
                            r="15.915"
                            fill="none"
                            stroke="rgb(var(--color-burgundy))"
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
                          <span className="text-2xl font-black text-gray-800">{totalSettlements}</span>
                          <span className="text-[10px] uppercase font-bold tracking-widest text-gray-400">Total</span>
                        </div>
                      </div>

                      <div className="flex flex-col gap-3">
                        <div className="flex items-center gap-3">
                          <span className="w-3.5 h-3.5 rounded-full bg-[rgb(var(--color-burgundy))] flex-shrink-0"></span>
                          <div className="flex flex-col">
                            <span className="text-sm font-semibold text-gray-700">Paid Invoices</span>
                            <span className="text-xs font-medium text-gray-400">
                              {billMetrics.paidSettlementsCount} settlements ({Math.round(paidPct)}%)
                            </span>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="w-3.5 h-3.5 rounded-full bg-amber-500 flex-shrink-0"></span>
                          <div className="flex flex-col">
                            <span className="text-sm font-semibold text-gray-700">Unpaid/Issued</span>
                            <span className="text-xs font-medium text-gray-400">
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
                      <p className="text-sm font-semibold text-gray-400">No Settlement Data Available</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="bg-white p-6 sm:p-8 rounded-3xl border border-[#DACDCA]/30 shadow-sm flex flex-col justify-between h-96">
                <div>
                  <h3 className="text-lg font-bold text-[rgb(var(--color-burgundy))]">
                    Financial Volume Summary
                  </h3>
                  <p className="text-xs text-gray-400 mt-1">
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
                        <div className="w-full bg-gray-100 h-6 rounded-xl overflow-hidden relative shadow-inner">
                          <div 
                            className="bg-emerald-500 h-full rounded-xl transition-all duration-1000 ease-out shadow-sm"
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
                        <div className="w-full bg-gray-100 h-6 rounded-xl overflow-hidden relative shadow-inner">
                          <div 
                            className="bg-amber-500 h-full rounded-xl transition-all duration-1000 ease-out shadow-sm"
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

                      <div className="border-t border-gray-100 pt-4 flex justify-between text-xs text-gray-400 font-medium">
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
                      <p className="text-sm font-semibold text-gray-400">No Revenue Data Available</p>
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
