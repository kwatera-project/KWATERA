import React, { useEffect, useState } from "react";
import {
    Users,
    UserCheck,
    Home,
    Search,
    X,
    ChevronLeft,
    ChevronRight,
    Calendar,
    Mail,
    RefreshCw
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { getAdminUsers, getAdminUserKpis, type AdminUser, type AdminUserKpis } from "../api/adminApi";

export default function AdminUsersOverviewPage() {
    const { t, i18n } = useTranslation();

    const [kpis, setKpis] = useState<AdminUserKpis | null>(null);
    const [loadingKpis, setLoadingKpis] = useState(true);

    const [users, setUsers] = useState<AdminUser[]>([]);
    const [loadingUsers, setLoadingUsers] = useState(true);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [searchQuery, setSearchQuery] = useState("");
    const [debouncedSearch, setDebouncedSearch] = useState("");
    const [selectedRole, setSelectedRole] = useState("ALL");
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize] = useState(10);

    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedSearch(searchQuery);
            setCurrentPage(0);
            setLoadingUsers(true);
        }, 300);

        return () => clearTimeout(handler);
    }, [searchQuery]);

    useEffect(() => {
        let active = true;
        getAdminUserKpis()
            .then((data) => {
                if (active) {
                    setKpis(data);
                    setLoadingKpis(false);
                }
            })
            .catch((err) => {
                console.error("Failed to load KPIs", err);
                if (active) {
                    setLoadingKpis(false);
                }
            });
        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        let active = true;
        getAdminUsers(currentPage, pageSize, selectedRole, debouncedSearch)
            .then((data) => {
                if (active) {
                    setUsers(data.content);
                    setTotalPages(data.totalPages);
                    setTotalElements(data.totalElements);
                    setLoadingUsers(false);
                }
            })
            .catch((err) => {
                const message = err instanceof Error ? err.message : t("adminUsers.loadUsersFailed");
                if (active) {
                    setError(message);
                    setLoadingUsers(false);
                }
            });
        return () => {
            active = false;
        };
    }, [currentPage, pageSize, selectedRole, debouncedSearch, t]);

    const handleRoleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedRole(e.target.value);
        setCurrentPage(0);
        setLoadingUsers(true);
    };

    const handleClearSearch = () => {
        setSearchQuery("");
        setLoadingUsers(true);
    };

    const handlePageChange = (newPage: number) => {
        if (newPage >= 0 && newPage < totalPages) {
            setCurrentPage(newPage);
            setLoadingUsers(true);
        }
    };

    const handleRefresh = () => {
        setLoadingKpis(true);
        setLoadingUsers(true);

        getAdminUserKpis()
            .then((data) => {
                setKpis(data);
                setLoadingKpis(false);
            })
            .catch((err) => {
                console.error("Failed to load KPIs", err);
                setLoadingKpis(false);
            });

        getAdminUsers(currentPage, pageSize, selectedRole, debouncedSearch)
            .then((data) => {
                setUsers(data.content);
                setTotalPages(data.totalPages);
                setTotalElements(data.totalElements);
                setLoadingUsers(false);
            })
            .catch((err) => {
                const message = err instanceof Error ? err.message : t("adminUsers.loadUsersFailed");
                setError(message);
                setLoadingUsers(false);
            });
    };

    const formatDate = (dateString?: string) => {
        if (!dateString) return t("common.notAvailable");
        const locale = i18n.language === "pl" ? "pl-PL" : "en-US";
        return new Date(dateString).toLocaleDateString(locale, {
            year: "numeric",
            month: "short",
            day: "numeric",
        });
    };

    const getRoleLabel = (role: string) => {
        return t(`adminUsers.roles.${role}`, { defaultValue: role });
    };

    const getStatusLabel = (status: string) => {
        return t(`adminUsers.statuses.${status}`, { defaultValue: status });
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-8">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-[#DACDCA] pb-6">
                <div className="space-y-2">
                    <h1 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight flex items-center gap-3">
                        <Users className="text-[#42211D] stroke-[2.5]" size={32} />
                        {t("adminUsers.title")}
                    </h1>
                    <p className="text-sm text-[#7A7A7A]">
                        {t("adminUsers.subtitle")}
                    </p>
                </div>

                <div className="flex items-end gap-3 ml-auto md:ml-0">
                    <button
                        onClick={handleRefresh}
                        className="px-5 py-2 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm cursor-pointer flex items-center gap-2"
                    >
                        <RefreshCw size={16} className={`${loadingUsers || loadingKpis ? 'animate-spin' : ''}`} />
                        {t("adminUsers.refresh")}
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-[#42211D]/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                    <div className="flex flex-col justify-between h-full space-y-4">
                        <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                                {t("adminUsers.kpi.totalUsers")}
                            </span>
                            <span className="p-2 bg-[#42211D]/10 text-[#42211D] rounded-xl">
                                <Users className="w-5 h-5" />
                            </span>
                        </div>
                        <div>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-[#F7F7F7] rounded animate-pulse"></div>
                            ) : (
                                <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                                    {kpis?.totalUsers || 0}
                                </h2>
                            )}
                            <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                                {t("adminUsers.kpi.totalUsersDesc")}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                    <div className="flex flex-col justify-between h-full space-y-4">
                        <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                                {t("adminUsers.kpi.guests")}
                            </span>
                            <span className="p-2 bg-blue-100 text-blue-700 rounded-xl">
                                <UserCheck className="w-5 h-5" />
                            </span>
                        </div>
                        <div>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-[#F7F7F7] rounded animate-pulse"></div>
                            ) : (
                                <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                                    {kpis?.totalGuests || 0}
                                </h2>
                            )}
                            <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                                {t("adminUsers.kpi.guestsDesc")}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                    <div className="flex flex-col justify-between h-full space-y-4">
                        <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                                {t("adminUsers.kpi.propertyOwners")}
                            </span>
                            <span className="p-2 bg-emerald-100 text-emerald-700 rounded-xl">
                                <Users className="w-5 h-5" />
                            </span>
                        </div>
                        <div>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-[#F7F7F7] rounded animate-pulse"></div>
                            ) : (
                                <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                                    {kpis?.totalOwners || 0}
                                </h2>
                            )}
                            <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                                {t("adminUsers.kpi.propertyOwnersDesc")}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="group bg-white p-6 rounded-xl border border-[#DACDCA] shadow-sm hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-bl-full pointer-events-none transition-all duration-300 group-hover:scale-110"></div>
                    <div className="flex flex-col justify-between h-full space-y-4">
                        <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                                {t("adminUsers.kpi.properties")}
                            </span>
                            <span className="p-2 bg-amber-100 text-amber-700 rounded-xl">
                                <Home className="w-5 h-5" />
                            </span>
                        </div>
                        <div>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-[#F7F7F7] rounded animate-pulse"></div>
                            ) : (
                                <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">
                                    {kpis?.totalProperties || 0}
                                </h2>
                            )}
                            <p className="text-xs text-[#7A7A7A] mt-2 font-medium">
                                {t("adminUsers.kpi.propertiesDesc")}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            <div className="bg-white border border-[#DACDCA] rounded-xl p-5 shadow-sm">
                <div className="flex flex-col sm:flex-row items-center gap-4 justify-between">
                    <div className="relative w-full sm:max-w-md">
                        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-stone-400" />
                        <input
                            type="text"
                            placeholder={t("adminUsers.searchPlaceholder")}
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-[#F7F7F7] border border-[#DACDCA] focus:border-[#42211D] focus:bg-white pl-11 pr-10 py-2.5 rounded-lg text-sm font-semibold transition-all duration-200 focus:outline-none focus:ring-1 focus:ring-[#42211D]"
                        />
                        {searchQuery && (
                            <button
                                onClick={handleClearSearch}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#7A7A7A] hover:text-[#1A1A1A] p-1 hover:bg-[#F7F7F7] rounded-full transition-colors"
                            >
                                <X size={14} />
                            </button>
                        )}
                    </div>

                    <div className="flex items-center gap-3 w-full sm:w-auto">
                        <label className="text-sm font-bold text-[#7A7A7A] uppercase tracking-wider hidden sm:block">
                            {t("adminUsers.roleLabel")}
                        </label>
                        <select
                            value={selectedRole}
                            onChange={handleRoleChange}
                            className="w-full sm:w-48 bg-[#F7F7F7] border border-[#DACDCA] hover:border-[#7A7A7A] py-2.5 px-4 rounded-lg text-sm font-bold text-[#1A1A1A] transition-all duration-200 focus:outline-none focus:bg-white focus:ring-1 focus:ring-[#42211D]"
                        >
                            <option value="ALL">{t("adminUsers.roles.ALL")}</option>
                            <option value="GUEST">{t("adminUsers.roles.GUEST")}</option>
                            <option value="OWNER">{t("adminUsers.roles.OWNER")}</option>
                        </select>
                    </div>
                </div>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-6 py-4 rounded-xl mb-6 flex items-center gap-3">
                    <span className="font-semibold">{error}</span>
                </div>
            )}

            <div className="block md:hidden space-y-4 mb-6">
                {loadingUsers ? (
                    Array.from({ length: 3 }).map((_, idx) => (
                        <div key={idx} className="bg-white border border-[#DACDCA] rounded-xl p-5 animate-pulse">
                            <div className="flex justify-between items-center mb-3">
                                <div className="h-5 bg-[#F7F7F7] rounded w-28"></div>
                                <div className="h-6 bg-[#F7F7F7] rounded-full w-16"></div>
                            </div>
                            <div className="space-y-2">
                                <div className="h-4 bg-[#F7F7F7] rounded w-48"></div>
                                <div className="h-4 bg-[#F7F7F7] rounded w-32"></div>
                            </div>
                        </div>
                    ))
                ) : users.length === 0 ? (
                    <div className="bg-white border border-[#DACDCA] rounded-xl p-8 text-center">
                        <Users className="mx-auto text-[#DACDCA] mb-2" size={36} />
                        <h3 className="text-[#1A1A1A] font-bold text-sm">{t("adminUsers.noUsersFound")}</h3>
                    </div>
                ) : (
                    users.map((user) => (
                        <div key={user.id} className="bg-white border border-[#DACDCA] rounded-xl p-5 shadow-sm hover:shadow hover:border-[#7A7A7A] transition-all duration-200">
                            <div className="flex justify-between items-start mb-3">
                                <div>
                                    <h4 className="font-bold text-[#1A1A1A] text-base">{user.firstName || "-"} {user.lastName || "-"}</h4>
                                    <div className="flex items-center gap-2 text-[#7A7A7A] text-xs mt-1">
                                        <Mail size={12} className="text-[#DACDCA]" />
                                        <span className="break-all">{user.email}</span>
                                    </div>
                                </div>
                                <span
                                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                                        user.role === "ADMIN"
                                            ? "bg-red-50 text-red-800 border border-red-200"
                                            : user.role === "OWNER"
                                              ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                                              : "bg-blue-50 text-blue-800 border border-blue-200"
                                    }`}
                                >
                                    {getRoleLabel(user.role)}
                                </span>
                            </div>

                            <div className="h-px bg-[#DACDCA]/50 my-3"></div>

                            <div className="flex justify-between items-center text-xs">
                                <span
                                    className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full font-bold ${
                                        user.status === "Active"
                                            ? "bg-emerald-100 text-emerald-800"
                                            : "bg-[#F7F7F7] text-[#7A7A7A] border border-[#DACDCA]"
                                    }`}
                                >
                                    <span className={`w-1.5 h-1.5 rounded-full ${user.status === "Active" ? 'bg-emerald-600' : 'bg-stone-400'}`}></span>
                                    {getStatusLabel(user.status)}
                                </span>

                                <div className="text-[#7A7A7A] flex items-center gap-1 font-medium">
                                    <Calendar size={12} />
                                    <span>{formatDate(user.createdAt)}</span>
                                </div>
                            </div>

                            {user.role === "OWNER" && (
                                <div className="mt-3 pt-3 border-t border-[#DACDCA]/50 flex justify-between items-center text-xs">
                                    <span className="text-[#7A7A7A] font-bold">{t("adminUsers.propertiesManaged")}</span>
                                    <span className="bg-[#F7F7F7] border border-[#DACDCA] px-2.5 py-0.5 rounded font-black text-[#1A1A1A]">
                                        {user.propertyCount}
                                    </span>
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>

            <div className="hidden md:block bg-white border border-[#DACDCA] rounded-xl shadow-sm overflow-hidden mb-6">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#F7F7F7] border-b border-[#DACDCA] text-[#7A7A7A] font-bold text-xs uppercase tracking-wider">
                                <th className="py-4.5 px-6">{t("adminUsers.tableHeaders.firstName")}</th>
                                <th className="py-4.5 px-6">{t("adminUsers.tableHeaders.lastName")}</th>
                                <th className="py-4.5 px-6">{t("adminUsers.tableHeaders.email")}</th>
                                <th className="py-4.5 px-6">{t("adminUsers.tableHeaders.role")}</th>
                                <th className="py-4.5 px-6">{t("adminUsers.tableHeaders.status")}</th>
                                <th className="py-4.5 px-6">{t("adminUsers.tableHeaders.createdDate")}</th>
                                <th className="py-4.5 px-6 text-right">{t("adminUsers.tableHeaders.properties")}</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-[#DACDCA]/50 text-sm font-medium text-[#1A1A1A]">
                            {loadingUsers ? (
                                Array.from({ length: 5 }).map((_, idx) => (
                                    <tr key={idx} className="animate-pulse">
                                        <td className="py-5 px-6"><div className="h-4 bg-[#F7F7F7] rounded w-24"></div></td>
                                        <td className="py-5 px-6"><div className="h-4 bg-[#F7F7F7] rounded w-24"></div></td>
                                        <td className="py-5 px-6"><div className="h-4 bg-[#F7F7F7] rounded w-48"></div></td>
                                        <td className="py-5 px-6"><div className="h-6 bg-[#F7F7F7] rounded-full w-16"></div></td>
                                        <td className="py-5 px-6"><div className="h-5 bg-[#F7F7F7] rounded-full w-14"></div></td>
                                        <td className="py-5 px-6"><div className="h-4 bg-[#F7F7F7] rounded w-28"></div></td>
                                        <td className="py-5 px-6 text-right"><div className="h-4 bg-[#F7F7F7] rounded w-8 ml-auto"></div></td>
                                    </tr>
                                ))
                            ) : users.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-16 text-center">
                                        <Users className="mx-auto text-[#DACDCA] mb-3 stroke-[1.5]" size={40} />
                                        <h3 className="text-[#1A1A1A] font-bold text-base">{t("adminUsers.noUsersFound")}</h3>
                                        <p className="text-[#7A7A7A] text-xs mt-1">
                                            {t("adminUsers.noUsersMatchSearch")}
                                        </p>
                                    </td>
                                </tr>
                            ) : (
                                users.map((user) => (
                                    <tr key={user.id} className="hover:bg-[#F7F7F7]/50 transition-colors border-b border-[#DACDCA]/30">
                                        <td className="py-4 px-6 text-[#1A1A1A] font-bold">{user.firstName || "-"}</td>
                                        <td className="py-4 px-6 text-[#1A1A1A] font-bold">{user.lastName || "-"}</td>
                                        <td className="py-4 px-6 text-[#7A7A7A] font-normal">
                                            <div className="flex items-center gap-2">
                                                <Mail size={14} className="text-[#DACDCA]" />
                                                {user.email}
                                            </div>
                                        </td>
                                        <td className="py-4 px-6">
                                            <span
                                                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border uppercase tracking-wider ${
                                                    user.role === "ADMIN"
                                                        ? "bg-red-50 text-red-800 border-red-200"
                                                        : user.role === "OWNER"
                                                          ? "bg-emerald-50 text-emerald-800 border-emerald-200"
                                                          : "bg-blue-50 text-blue-800 border-blue-200"
                                                }`}
                                            >
                                                {getRoleLabel(user.role)}
                                            </span>
                                        </td>
                                        <td className="py-4 px-6">
                                            <span
                                                className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-bold ${
                                                    user.status === "Active"
                                                        ? "bg-emerald-100 text-emerald-800"
                                                        : "bg-[#F7F7F7] text-[#7A7A7A] border border-[#DACDCA]"
                                                }`}
                                            >
                                                <span className={`w-1.5 h-1.5 rounded-full ${user.status === "Active" ? 'bg-emerald-600' : 'bg-stone-400'}`}></span>
                                                {getStatusLabel(user.status)}
                                            </span>
                                        </td>
                                        <td className="py-4 px-6 text-[#7A7A7A] font-normal">
                                            <div className="flex items-center gap-2">
                                                <Calendar size={14} className="text-[#DACDCA]" />
                                                {formatDate(user.createdAt)}
                                            </div>
                                        </td>
                                        <td className="py-4 px-6 text-right font-black text-[#1A1A1A]">
                                            {user.role === "OWNER" ? (
                                                <span className="bg-[#F7F7F7] border border-[#DACDCA] px-2.5 py-1 rounded text-xs font-black">
                                                    {user.propertyCount}
                                                </span>
                                            ) : (
                                                <span className="text-[#DACDCA]">-</span>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {!loadingUsers && totalPages > 0 && (
                    <div className="bg-[#F7F7F7] border-t border-[#DACDCA] px-6 py-4 flex items-center justify-between">
                        <div className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                            {t("adminUsers.showing")} <span className="text-[#1A1A1A]">{users.length}</span> {t("adminUsers.of")}{" "}
                            <span className="text-[#1A1A1A]">{totalElements}</span> {t("adminUsers.users")}
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                onClick={() => handlePageChange(currentPage - 1)}
                                disabled={currentPage === 0}
                                className="p-2 border border-[#DACDCA] rounded-lg bg-white hover:bg-[#F7F7F7] disabled:opacity-40 disabled:hover:bg-white transition-all cursor-pointer"
                            >
                                <ChevronLeft size={16} />
                            </button>
                            <span className="text-xs font-bold text-[#7A7A7A] px-2">
                                {t("adminUsers.page")} {currentPage + 1} {t("adminUsers.of2")} {totalPages}
                            </span>
                            <button
                                onClick={() => handlePageChange(currentPage + 1)}
                                disabled={currentPage === totalPages - 1}
                                className="p-2 border border-[#DACDCA] rounded-lg bg-white hover:bg-[#F7F7F7] disabled:opacity-40 disabled:hover:bg-white transition-all cursor-pointer"
                            >
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {!loadingUsers && totalPages > 0 && (
                <div className="flex md:hidden bg-[#F7F7F7] border border-[#DACDCA] rounded-xl px-4 py-3 items-center justify-between">
                    <div className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                        {t("adminUsers.total")} <span className="text-[#1A1A1A]">{totalElements}</span> {t("adminUsers.users")}
                    </div>
                    <div className="flex items-center gap-1">
                        <button
                            onClick={() => handlePageChange(currentPage - 1)}
                            disabled={currentPage === 0}
                            className="p-2 border border-[#DACDCA] rounded-lg bg-white hover:bg-[#F7F7F7] disabled:opacity-40 transition-all cursor-pointer"
                        >
                            <ChevronLeft size={14} />
                        </button>
                        <span className="text-xs font-bold text-[#7A7A7A] px-1">
                            {currentPage + 1} / {totalPages}
                        </span>
                        <button
                            onClick={() => handlePageChange(currentPage + 1)}
                            disabled={currentPage === totalPages - 1}
                            className="p-2 border border-[#DACDCA] rounded-lg bg-white hover:bg-[#F7F7F7] disabled:opacity-40 transition-all cursor-pointer"
                        >
                            <ChevronRight size={14} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
