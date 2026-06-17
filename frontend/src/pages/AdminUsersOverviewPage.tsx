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
import { getAdminUsers, getAdminUserKpis, type AdminUser, type AdminUserKpis } from "../api/adminApi";

export default function AdminUsersOverviewPage() {
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
        }, 300);

        return () => clearTimeout(handler);
    }, [searchQuery]);

    const fetchKpis = async () => {
        setLoadingKpis(true);
        try {
            const data = await getAdminUserKpis();
            setKpis(data);
        } catch (err) {
            console.error("Failed to load KPIs", err);
        } finally {
            setLoadingKpis(false);
        }
    };

    const fetchUsers = async () => {
        setLoadingUsers(true);
        setError(null);
        try {
            const data = await getAdminUsers(currentPage, pageSize, selectedRole, debouncedSearch);
            setUsers(data.content);
            setTotalPages(data.totalPages);
            setTotalElements(data.totalElements);
        } catch (err: any) {
            setError(err.message || "Failed to load users list");
        } finally {
            setLoadingUsers(false);
        }
    };

    useEffect(() => {
        fetchKpis();
    }, []);

    useEffect(() => {
        fetchUsers();
    }, [currentPage, selectedRole, debouncedSearch]);

    const handleRoleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedRole(e.target.value);
        setCurrentPage(0);
    };

    const handleClearSearch = () => {
        setSearchQuery("");
    };

    const handlePageChange = (newPage: number) => {
        if (newPage >= 0 && newPage < totalPages) {
            setCurrentPage(newPage);
        }
    };

    const formatDate = (dateString?: string) => {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
        });
    };

    return (
        <div className="p-4 md:p-8 max-w-7xl mx-auto min-h-screen text-stone-800">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
                <div>
                    <h1 className="text-2xl md:text-3xl font-extrabold text-stone-900 tracking-tight flex items-center gap-3">
                        <Users className="text-[rgb(var(--color-burgundy))] stroke-[2.5]" size={32} />
                        Users Overview
                    </h1>
                    <p className="text-sm md:text-base text-stone-500 mt-1">
                        Monitor system accounts, role distributions, and managed properties in real-time.
                    </p>
                </div>
                
                <button
                    onClick={() => {
                        fetchKpis();
                        fetchUsers();
                    }}
                    className="self-start sm:self-auto flex items-center gap-2 px-4 py-2 border border-gray-200 hover:border-gray-300 bg-white hover:bg-gray-50 rounded-xl shadow-sm text-sm font-semibold transition-all duration-200"
                >
                    <RefreshCw size={16} className={`${loadingUsers || loadingKpis ? 'animate-spin' : ''}`} />
                    Refresh
                </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <div className="bg-white border border-stone-200/80 rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-stone-50 rounded-bl-full -z-10 group-hover:bg-amber-50/40 transition-colors duration-300"></div>
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-stone-400 font-semibold uppercase text-xs tracking-wider">Total Users</p>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-stone-100 animate-pulse rounded mt-2"></div>
                            ) : (
                                <h3 className="text-3xl font-black text-stone-950 mt-1">{kpis?.totalUsers || 0}</h3>
                            )}
                        </div>
                        <div className="bg-stone-100 text-stone-600 p-3 rounded-xl group-hover:bg-[rgb(var(--color-burgundy))] group-hover:text-white transition-colors duration-300">
                            <Users size={20} />
                        </div>
                    </div>
                    <div className="mt-4 flex items-center text-xs text-stone-500">
                        <span className="font-medium">All registered accounts</span>
                    </div>
                </div>

                <div className="bg-white border border-stone-200/80 rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-stone-50 rounded-bl-full -z-10 group-hover:bg-blue-50/40 transition-colors duration-300"></div>
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-stone-400 font-semibold uppercase text-xs tracking-wider">Guests</p>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-stone-100 animate-pulse rounded mt-2"></div>
                            ) : (
                                <h3 className="text-3xl font-black text-stone-950 mt-1">{kpis?.totalGuests || 0}</h3>
                            )}
                        </div>
                        <div className="bg-stone-100 text-stone-600 p-3 rounded-xl group-hover:bg-blue-600 group-hover:text-white transition-colors duration-300">
                            <UserCheck size={20} />
                        </div>
                    </div>
                    <div className="mt-4 flex items-center text-xs text-stone-500">
                        <span className="font-medium">Clients placing reservations</span>
                    </div>
                </div>

                <div className="bg-white border border-stone-200/80 rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-stone-50 rounded-bl-full -z-10 group-hover:bg-emerald-50/40 transition-colors duration-300"></div>
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-stone-400 font-semibold uppercase text-xs tracking-wider">Property Owners</p>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-stone-100 animate-pulse rounded mt-2"></div>
                            ) : (
                                <h3 className="text-3xl font-black text-stone-950 mt-1">{kpis?.totalOwners || 0}</h3>
                            )}
                        </div>
                        <div className="bg-stone-100 text-stone-600 p-3 rounded-xl group-hover:bg-emerald-600 group-hover:text-white transition-colors duration-300">
                            <Users size={20} />
                        </div>
                    </div>
                    <div className="mt-4 flex items-center text-xs text-stone-500">
                        <span className="font-medium">Managing listings & rentals</span>
                    </div>
                </div>

                <div className="bg-white border border-stone-200/80 rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-stone-50 rounded-bl-full -z-10 group-hover:bg-indigo-50/40 transition-colors duration-300"></div>
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-stone-400 font-semibold uppercase text-xs tracking-wider">Properties</p>
                            {loadingKpis ? (
                                <div className="h-8 w-16 bg-stone-100 animate-pulse rounded mt-2"></div>
                            ) : (
                                <h3 className="text-3xl font-black text-stone-950 mt-1">{kpis?.totalProperties || 0}</h3>
                            )}
                        </div>
                        <div className="bg-stone-100 text-stone-600 p-3 rounded-xl group-hover:bg-indigo-600 group-hover:text-white transition-colors duration-300">
                            <Home size={20} />
                        </div>
                    </div>
                    <div className="mt-4 flex items-center text-xs text-stone-500">
                        <span className="font-medium">Properties listed on KWATERA</span>
                    </div>
                </div>
            </div>

            <div className="bg-white border border-stone-200 rounded-2xl p-5 mb-6 shadow-sm">
                <div className="flex flex-col sm:flex-row items-center gap-4 justify-between">
                    <div className="relative w-full sm:max-w-md">
                        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-stone-400" />
                        <input
                            type="text"
                            placeholder="Search by name or email..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-stone-50 border border-stone-200 focus:border-stone-300 focus:bg-white pl-11 pr-10 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 focus:outline-none"
                        />
                        {searchQuery && (
                            <button
                                onClick={handleClearSearch}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600 p-1 hover:bg-stone-100 rounded-full transition-colors"
                            >
                                <X size={14} />
                            </button>
                        )}
                    </div>

                    <div className="flex items-center gap-3 w-full sm:w-auto">
                        <label className="text-sm font-semibold text-stone-500 hidden sm:block">Role:</label>
                        <select
                            value={selectedRole}
                            onChange={handleRoleChange}
                            className="w-full sm:w-48 bg-stone-50 border border-stone-200 hover:border-stone-300 py-2.5 px-4 rounded-xl text-sm font-semibold transition-all duration-200 focus:outline-none focus:bg-white"
                        >
                            <option value="ALL">All Roles</option>
                            <option value="GUEST">Guest</option>
                            <option value="OWNER">Owner</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                    </div>
                </div>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-6 py-4 rounded-2xl mb-6 flex items-center gap-3">
                    <span className="font-semibold">{error}</span>
                </div>
            )}

            <div className="block md:hidden space-y-4 mb-6">
                {loadingUsers ? (
                    Array.from({ length: 3 }).map((_, idx) => (
                        <div key={idx} className="bg-white border border-stone-200 rounded-xl p-5 animate-pulse">
                            <div className="flex justify-between items-center mb-3">
                                <div className="h-5 bg-stone-100 rounded w-28"></div>
                                <div className="h-6 bg-stone-100 rounded-full w-16"></div>
                            </div>
                            <div className="space-y-2">
                                <div className="h-4 bg-stone-100 rounded w-48"></div>
                                <div className="h-4 bg-stone-100 rounded w-32"></div>
                            </div>
                        </div>
                    ))
                ) : users.length === 0 ? (
                    <div className="bg-white border border-stone-200 rounded-xl p-8 text-center">
                        <Users className="mx-auto text-stone-300 mb-2" size={36} />
                        <h3 className="text-stone-900 font-bold text-sm">No Users Found</h3>
                    </div>
                ) : (
                    users.map((user) => (
                        <div key={user.id} className="bg-white border border-stone-200 rounded-xl p-5 shadow-sm hover:shadow transition-all duration-200">
                            <div className="flex justify-between items-start mb-3">
                                <div>
                                    <h4 className="font-bold text-stone-950 text-base">{user.firstName || "-"} {user.lastName || "-"}</h4>
                                    <div className="flex items-center gap-2 text-stone-500 text-xs mt-1">
                                        <Mail size={12} className="text-stone-400" />
                                        <span className="break-all">{user.email}</span>
                                    </div>
                                </div>
                                <span 
                                    className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                                        user.role === "ADMIN" 
                                            ? "bg-rose-50 text-rose-700 border border-rose-100" 
                                            : user.role === "OWNER"
                                              ? "bg-emerald-50 text-emerald-700 border border-emerald-100"
                                              : "bg-blue-50 text-blue-700 border border-blue-100"
                                    }`}
                                >
                                    {user.role}
                                </span>
                            </div>
                            
                            <div className="h-px bg-stone-100 my-3"></div>
                            
                            <div className="flex justify-between items-center text-xs">
                                <span 
                                    className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full font-semibold ${
                                        user.status === "Active" 
                                            ? "bg-green-100 text-green-800" 
                                            : "bg-stone-100 text-stone-600"
                                    }`}
                                >
                                    <span className={`w-1.5 h-1.5 rounded-full ${user.status === "Active" ? 'bg-green-600' : 'bg-stone-400'}`}></span>
                                    {user.status}
                                </span>
                                
                                <div className="text-stone-500 flex items-center gap-1">
                                    <Calendar size={12} />
                                    <span>{formatDate(user.createdAt)}</span>
                                </div>
                            </div>

                            {user.role === "OWNER" && (
                                <div className="mt-3 pt-3 border-t border-stone-100 flex justify-between items-center text-xs">
                                    <span className="text-stone-500 font-medium">Properties managed:</span>
                                    <span className="bg-stone-100 px-2 py-0.5 rounded font-black text-stone-900">
                                        {user.propertyCount}
                                    </span>
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>

            <div className="hidden md:block bg-white border border-stone-200 rounded-2xl shadow-sm overflow-hidden mb-6">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-stone-50 border-b border-stone-100 text-stone-500 font-semibold text-xs uppercase tracking-wider">
                                <th className="py-4.5 px-6 font-bold">First Name</th>
                                <th className="py-4.5 px-6 font-bold">Last Name</th>
                                <th className="py-4.5 px-6 font-bold">Email</th>
                                <th className="py-4.5 px-6 font-bold">Role</th>
                                <th className="py-4.5 px-6 font-bold">Status</th>
                                <th className="py-4.5 px-6 font-bold">Created Date</th>
                                <th className="py-4.5 px-6 font-bold text-right">Properties</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-stone-100 text-sm font-medium text-stone-700">
                            {loadingUsers ? (
                                Array.from({ length: 5 }).map((_, idx) => (
                                    <tr key={idx} className="animate-pulse">
                                        <td className="py-5 px-6"><div className="h-4 bg-stone-100 rounded w-24"></div></td>
                                        <td className="py-5 px-6"><div className="h-4 bg-stone-100 rounded w-24"></div></td>
                                        <td className="py-5 px-6"><div className="h-4 bg-stone-100 rounded w-48"></div></td>
                                        <td className="py-5 px-6"><div className="h-6 bg-stone-100 rounded-full w-16"></div></td>
                                        <td className="py-5 px-6"><div className="h-5 bg-stone-100 rounded-full w-14"></div></td>
                                        <td className="py-5 px-6"><div className="h-4 bg-stone-100 rounded w-28"></div></td>
                                        <td className="py-5 px-6 text-right"><div className="h-4 bg-stone-100 rounded w-8 ml-auto"></div></td>
                                    </tr>
                                ))
                            ) : users.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-16 text-center">
                                        <Users className="mx-auto text-stone-300 mb-3 stroke-[1.5]" size={40} />
                                        <h3 className="text-stone-900 font-bold text-base">No Users Found</h3>
                                        <p className="text-stone-400 text-xs mt-1">
                                            No user accounts match your search parameters.
                                        </p>
                                    </td>
                                </tr>
                            ) : (
                                users.map((user) => (
                                    <tr key={user.id} className="hover:bg-stone-50/50 transition-colors">
                                        <td className="py-4 px-6 text-stone-900 font-semibold">{user.firstName || "-"}</td>
                                        <td className="py-4 px-6 text-stone-900 font-semibold">{user.lastName || "-"}</td>
                                        <td className="py-4 px-6 text-stone-500 font-normal">
                                            <div className="flex items-center gap-2">
                                                <Mail size={14} className="text-stone-400" />
                                                {user.email}
                                            </div>
                                        </td>
                                        <td className="py-4 px-6">
                                            <span 
                                                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider ${
                                                    user.role === "ADMIN" 
                                                        ? "bg-rose-50 text-rose-700 border border-rose-100" 
                                                        : user.role === "OWNER"
                                                          ? "bg-emerald-50 text-emerald-700 border border-emerald-100"
                                                          : "bg-blue-50 text-blue-700 border border-blue-100"
                                                }`}
                                            >
                                                {user.role}
                                            </span>
                                        </td>
                                        <td className="py-4 px-6">
                                            <span 
                                                className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-semibold ${
                                                    user.status === "Active" 
                                                        ? "bg-green-100 text-green-800" 
                                                        : "bg-stone-100 text-stone-600"
                                                }`}
                                            >
                                                <span className={`w-1.5 h-1.5 rounded-full ${user.status === "Active" ? 'bg-green-600' : 'bg-stone-400'}`}></span>
                                                {user.status}
                                            </span>
                                        </td>
                                        <td className="py-4 px-6 text-stone-500 font-normal">
                                            <div className="flex items-center gap-2">
                                                <Calendar size={14} className="text-stone-400" />
                                                {formatDate(user.createdAt)}
                                            </div>
                                        </td>
                                        <td className="py-4 px-6 text-right font-bold text-stone-900">
                                            {user.role === "OWNER" ? (
                                                <span className="bg-stone-100 px-2.5 py-1 rounded-lg text-xs font-black">
                                                    {user.propertyCount}
                                                </span>
                                            ) : (
                                                <span className="text-stone-300">-</span>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {!loadingUsers && totalPages > 0 && (
                    <div className="bg-stone-50 border-t border-stone-100 px-6 py-4 flex items-center justify-between">
                        <div className="text-xs font-semibold text-stone-500">
                            Showing <span className="text-stone-800">{users.length}</span> of{" "}
                            <span className="text-stone-800">{totalElements}</span> users
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                onClick={() => handlePageChange(currentPage - 1)}
                                disabled={currentPage === 0}
                                className="p-2 border border-stone-200 rounded-lg hover:bg-white disabled:opacity-40 disabled:hover:bg-transparent transition-all"
                            >
                                <ChevronLeft size={16} />
                            </button>
                            <span className="text-xs font-semibold text-stone-600 px-2">
                                Page {currentPage + 1} of {totalPages}
                            </span>
                            <button
                                onClick={() => handlePageChange(currentPage + 1)}
                                disabled={currentPage === totalPages - 1}
                                className="p-2 border border-stone-200 rounded-lg hover:bg-white disabled:opacity-40 disabled:hover:bg-transparent transition-all"
                            >
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {!loadingUsers && totalPages > 0 && (
                <div className="flex md:hidden bg-stone-50 border border-stone-200 rounded-xl px-4 py-3 items-center justify-between">
                    <div className="text-xs font-semibold text-stone-500">
                        Total: <span className="text-stone-800">{totalElements}</span> users
                    </div>
                    <div className="flex items-center gap-1">
                        <button
                            onClick={() => handlePageChange(currentPage - 1)}
                            disabled={currentPage === 0}
                            className="p-2 border border-stone-200 rounded-lg hover:bg-white bg-white disabled:opacity-40 transition-all"
                        >
                            <ChevronLeft size={14} />
                        </button>
                        <span className="text-xs font-semibold text-stone-600 px-1">
                            {currentPage + 1} / {totalPages}
                        </span>
                        <button
                            onClick={() => handlePageChange(currentPage + 1)}
                            disabled={currentPage === totalPages - 1}
                            className="p-2 border border-stone-200 rounded-lg hover:bg-white bg-white disabled:opacity-40 transition-all"
                        >
                            <ChevronRight size={14} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
