import { useRef } from "react";
import DatePicker from "react-datepicker";
import SharedDatePicker from "../components/SharedDatePicker";
import { useTranslation } from "react-i18next";
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
import {
  Calendar,
  DollarSign,
  Percent,
  Activity,
  Shield,
  Loader2,
  AlertCircle
} from "lucide-react";
import ReportExportButtons from "../components/ReportExportButtons";
import { useDashboardData } from "../hooks/useDashboardData";
import { getLocaleCode } from "../utils/locale";

export default function DashboardPage() {
  const {
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
  } = useDashboardData();
  const dateToRef = useRef<DatePicker | null>(null);
  const {t, i18n} = useTranslation();
  const localeCode = getLocaleCode(i18n.language);
  const totalReservations = resMetrics.totalReservations;
  const occupancyRate = resMetrics.occupancyRate;
  const occupiedDays = resMetrics.occupiedDays;

  const revenueFromSettlements = billMetrics.revenueFromSettlements;
  const unpaidBalance = billMetrics.unpaidBalance;
  const paidSettlementsCount = billMetrics.paidSettlementsCount;
  const unpaidSettlementsCount = billMetrics.unpaidSettlementsCount;

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    refreshData();
  };

  const totalSettlements = paidSettlementsCount + unpaidSettlementsCount;
  const paidPct = totalSettlements > 0 ? (paidSettlementsCount / totalSettlements) * 100 : 0;

  return (
    <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-8">
      {/* Header section */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-[#DACDCA] pb-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">{t("dashboard.title")}</h1>
            {userRole && (
              <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider ${
                userRole === "ADMIN" 
                  ? "bg-red-50 text-red-800 border-red-200" 
                  : "bg-indigo-50 text-indigo-800 border-indigo-200"
              }`}>
                <Shield className="w-3.5 h-3.5" />
                  {userRole === "ADMIN" ? t("dashboard.globalAdministrator") : t("dashboard.propertyOwner")}
              </span>
            )}
          </div>
          <p className="text-sm text-[#7A7A7A]">
              {userRole === "ADMIN"
                  ? t("dashboard.adminDescription")
                  : t("dashboard.ownerDescription")}
          </p>
        </div>

        <div className="flex flex-col items-end gap-3 ml-auto">
          <form onSubmit={handleFilterSubmit} className="flex flex-wrap items-center gap-3 bg-white p-3 rounded-xl shadow-sm border border-[#DACDCA]">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t("dashboard.from")}</span>
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
                  placeholderText={t("dashboard.start")}
                  className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                  allowPastDates={true}
                />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t("dashboard.to")}</span>
              <div className="flex items-center bg-[#F7F7F7] border border-[#DACDCA] rounded-lg px-3 py-1.5 shadow-sm focus-within:ring-1 focus-within:ring-[#42211D]">
                <SharedDatePicker
                  datepickerRef={dateToRef}
                  selected={endDate}
                  onChange={(date) => setEndDate(date)}
                  selectsEnd
                  startDate={startDate}
                  endDate={endDate}
                  minDate={startDate}
                  placeholderText={t("dashboard.end")}
                  className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                  allowPastDates={true}
                />
              </div>
            </div>
            <button
              type="submit"
              className="px-5 py-2 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm cursor-pointer"
            >
                {t("dashboard.filter")}
            </button>
          </form>
          <div className="flex flex-wrap items-center gap-3 justify-end">
            <ReportExportButtons
              startDate={startDate}
              endDate={endDate}
              userRole={userRole}
              resMetrics={resMetrics}
              billMetrics={billMetrics}
              totalUnitsCount={totalUnitsCount}
              activeReservations={activeReservations}
            />
          </div>
        </div>
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
          {/* 1. Statistic tiles */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-[#42211D]/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {t("dashboard.reservations")}
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
                      {t("dashboard.activeBookings")}
                  </p>
                </div>
              </div>
            </div>

            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {t("dashboard.occupancyRate")}
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
                      {t("dashboard.totalNightsUnits", {nights: occupiedDays, units: totalUnitsCount
                      })}
                  </p>
                </div>
              </div>
            </div>

            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {t("dashboard.collectedRevenue")}
                  </span>
                  <span className="p-2 bg-blue-100 text-blue-700 rounded-xl">
                    <DollarSign className="w-5 h-5" />
                  </span>
                </div>
                <div>
                  <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                    {revenueFromSettlements.toLocaleString(localeCode, { style: "currency", currency: "PLN" })}
                  </h2>
                  <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                      {t("dashboard.paidSettlementsAndDeposits")}
                  </p>
                </div>
              </div>
            </div>

            <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
              <div className="flex flex-col justify-between h-full space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {t("dashboard.outstandingReceivables")}
                  </span>
                  <span className="p-2 bg-amber-100 text-amber-700 rounded-xl">
                    <Activity className="w-5 h-5" />
                  </span>
                </div>
                <div>
                  <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                    {unpaidBalance.toLocaleString(localeCode, { style: "currency", currency: "PLN" })}
                  </h2>
                  <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                      {t("dashboard.pendingInvoiceSettlements")}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* 2. Responsive CSS Grid for Core Analytics Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            
            {/* Chart 1: Occupancy Trend */}
            <div id="occupancy-chart" className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4 min-w-0 overflow-hidden">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                    {t("dashboard.occupancyTrend")}
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                    {t("dashboard.occupancyTrendDescription")}
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
                      {t("dashboard.noOccupancyData")}
                  </div>
                )}
              </div>
            </div>

            {/* Chart 2: Base vs. Sold Price Analysis */}
            <div className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4 min-w-0 overflow-hidden">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                    {t("dashboard.priceAnalysis")}
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                    {t("dashboard.priceAnalysisDescription")}
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
                      <Line type="monotone" dataKey="staticAdr" name={t("dashboard.staticBasePrice")}  stroke="#9CA3AF" strokeWidth={2} strokeDasharray="5 5" dot={false} activeDot={{ r: 4 }} />
                      <Line type="monotone" dataKey="aiAdr" name={t("dashboard.actualPriceAdr")} stroke="#6366F1" strokeWidth={2.5} dot={{ r: 3, strokeWidth: 1 }} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                      {t("dashboard.noPricingData")}
                  </div>
                )}
              </div>
            </div>

            {/* Chart 3: Net Profit & Utility Expenses */}
            <div id="revenue-chart" className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4 min-w-0 overflow-hidden">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                    {t("dashboard.netProfitUtilityExpenses")}
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                    {t("dashboard.netProfitDescription")}
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
                      <Bar dataKey="grossRevenue" name={t("dashboard.grossRevenue")} fill="#3B82F6" radius={[4, 4, 0, 0]} barSize={20} />
                      <Bar dataKey="utilityCosts" name={t("dashboard.utilityExpenses")} fill="#EF4444" radius={[4, 4, 0, 0]} barSize={20} />
                      <Line type="monotone" dataKey="netProfit" name={t("dashboard.netProfit")} stroke="#10B981" strokeWidth={3} dot={{ r: 4 }} />
                    </ComposedChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                      {t("dashboard.noFinancialHistory")}
                  </div>
                )}
              </div>
            </div>

            {/* Chart 4: Unit Performance Ranking */}
            <div className="bg-white rounded-xl border border-[#DACDCA] shadow-sm p-6 space-y-4 min-w-0 overflow-hidden">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                    {t("dashboard.unitProfitabilityRanking")}
                </h3>
                <p className="text-xs text-[#7A7A7A]">
                    {t("dashboard.unitRankingDescription")}
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
                      <Bar dataKey="revenue" name={t("dashboard.totalRevenue")} fill="#42211D" radius={[0, 4, 4, 0]} barSize={14} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm font-medium text-gray-400">
                      {t("dashboard.noActiveRentals")}
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
                    {t("dashboard.settlementsPaymentRatio")}
                </h3>
                <p className="text-xs text-[#7A7A7A] mt-1">
                    {t("dashboard.settlementsPaymentRatioDescription")}
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
                        <span className="text-[10px] uppercase font-bold tracking-widest text-[#7A7A7A]">{t("dashboard.total")}</span>
                      </div>
                    </div>

                    <div className="flex flex-col gap-3">
                      <div className="flex items-center gap-3">
                        <span className="w-3.5 h-3.5 rounded-full bg-[#42211D] flex-shrink-0"></span>
                        <div className="flex flex-col">
                          <span className="text-sm font-semibold text-gray-700">{t("dashboard.paidInvoices")}</span>
                          <span className="text-xs font-medium text-[#7A7A7A]">
                              {paidSettlementsCount}{" "}
                              {t("dashboard.settlement", { count: paidSettlementsCount })}{" "}
                              ({Math.round(paidPct)}%)
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="w-3.5 h-3.5 rounded-full bg-amber-500 flex-shrink-0"></span>
                        <div className="flex flex-col">
                          <span className="text-sm font-semibold text-gray-700">{t("dashboard.unpaidIssued")}</span>
                          <span className="text-xs font-medium text-[#7A7A7A]">
                            {unpaidSettlementsCount}{" "}
                              {t("dashboard.settlement", { count: unpaidSettlementsCount })}{" "}
                              ({Math.round(100 - paidPct)}%)
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
                    <p className="text-sm font-semibold text-[#7A7A7A]">{t("dashboard.noSettlementData")}</p>
                  </div>
                )}
              </div>
            </div>

            <div className="bg-white p-6 sm:p-8 rounded-xl border border-[#DACDCA] shadow-sm flex flex-col justify-between h-96">
              <div>
                <h3 className="text-lg font-bold text-[#1A1A1A]">
                    {t("dashboard.financialVolumeBreakdown")}
                </h3>
                <p className="text-xs text-[#7A7A7A] mt-1">
                    {t("dashboard.financialVolumeDescription")}
                </p>
              </div>

              <div className="flex flex-col justify-center gap-6 my-auto h-full w-full">
                {revenueFromSettlements > 0 || unpaidBalance > 0 ? (
                  <div className="space-y-6 w-full px-2 pt-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-semibold text-gray-700 flex items-center gap-2">
                          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                            {t("dashboard.collectedRevenue")}
                        </span>
                        <span className="font-bold text-gray-900">
                          {revenueFromSettlements.toLocaleString(localeCode, { style: "currency", currency: "PLN" })}
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
                            {t("dashboard.outstandingReceivables")}
                        </span>
                        <span className="font-bold text-gray-900">
                          {unpaidBalance.toLocaleString(localeCode, { style: "currency", currency: "PLN" })}
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
                        <span>{t("dashboard.totalSettlementsVolume")}</span>
                      <span>
                        {(revenueFromSettlements + unpaidBalance).toLocaleString(localeCode, { style: "currency", currency: "PLN" })}
                      </span>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-10 space-y-3">
                    <div className="mx-auto w-16 h-16 text-gray-200 bg-gray-50 rounded-full flex items-center justify-center">
                      <AlertCircle className="w-8 h-8" />
                    </div>
                    <p className="text-sm font-semibold text-[#7A7A7A]">{t("dashboard.noRevenueData")}</p>
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
    const {t} = useTranslation();
  if (active && payload && payload.length) {
    return (
      <div className="bg-white p-3.5 rounded-xl shadow-lg border border-gray-100 text-xs font-semibold space-y-1" style={{ backgroundColor: '#ffffff' }}>
        <p className="text-gray-400 font-bold uppercase tracking-wider text-[9px]">{label}</p>
        <p className="text-sm flex items-center gap-1.5 font-bold text-gray-900">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
            {t("dashboard.occupancy")}: <span className="text-emerald-600 text-base">{payload[0].value}%</span>
        </p>
      </div>
    );
  }
  return null;
};

const CustomPricingTooltip = ({ active, payload, label }: CustomTooltipProps) => {
    const {t, i18n} = useTranslation();
    const localeCode = getLocaleCode(i18n.language);
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
                {t("dashboard.baseStaticPrice")}:
            </span>
            <span>zł{Number(staticVal).toLocaleString(localeCode, { minimumFractionDigits: 2 })}</span>
          </p>
          <p className="flex items-center justify-between gap-4 font-semibold text-indigo-600">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-indigo-500"></span>
                {t("dashboard.actualSoldPriceAdr")}:
            </span>
            <span>zł{Number(aiVal).toLocaleString(localeCode, { minimumFractionDigits: 2 })}</span>
          </p>
          <div className="border-t border-gray-100 pt-1.5 mt-1.5 flex items-center justify-between gap-4 font-bold text-emerald-600">
            <span>{t("dashboard.pricingMargin")}:</span>
            <span>+zł{diff.toLocaleString(localeCode, { minimumFractionDigits: 2 })} ({pct.toFixed(1)}%)</span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

const CustomProfitTooltip = ({ active, payload, label }: CustomTooltipProps) => {
    const {t, i18n} = useTranslation();
    const localeCode = getLocaleCode(i18n.language);
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
                {t("dashboard.grossRevenue")}:
            </span>
            <span>zł{Number(gross).toLocaleString(localeCode)}</span>
          </p>
          <p className="flex items-center justify-between gap-4 font-semibold text-red-600">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                {t("dashboard.utilityCosts")}:
            </span>
            <span>zł{Number(utilities).toLocaleString(localeCode)}</span>
          </p>
          <div className="border-t border-gray-100 pt-1.5 mt-1.5 flex items-center justify-between gap-4 font-bold text-emerald-600">
            <span>{t("dashboard.netProfit")}:</span>
            <span>zł{net.toLocaleString(localeCode)}</span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

const CustomRankingTooltip = ({ active, payload }: CustomTooltipProps) => {
    const {t, i18n} = useTranslation();
    const localeCode = getLocaleCode(i18n.language);
    if (active && payload && payload.length) {
    const revenue = payload.find(p => p.dataKey === 'revenue')?.value ?? 0;
    const itemPayload = payload[0]?.payload || {};
    const uName = itemPayload.name || t("dashboard.unknownUnit");
    const bookings = itemPayload.bookings || 0;
    return (
      <div className="bg-white p-3.5 rounded-xl shadow-lg border border-gray-100 text-xs font-semibold space-y-1.5" style={{ backgroundColor: '#ffffff' }}>
        <p className="text-gray-900 font-extrabold">{uName}</p>
        <p className="text-sm font-bold text-gray-700 flex justify-between gap-4">
            <span>{t("dashboard.totalRevenue")}:</span>
          <span className="text-[#42211D]">zł{Number(revenue).toLocaleString(localeCode)}</span>
        </p>
          <p className="text-xs text-gray-500 flex justify-between gap-4">
              <span>{t("dashboard.bookingsCount")}:</span>
              <span>
                  {bookings} {t("dashboard.stay", { count: bookings })}
              </span>
          </p>
      </div>
    );
  }
  return null;
};
