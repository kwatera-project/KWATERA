import { useEffect, useState } from "react";
import { getUserProfile, updateUserProfile, type UserProfile } from "../api/userApi";

export default function ProfilePage() {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [isEditing, setIsEditing] = useState(false);
    const [editFirstName, setEditFirstName] = useState("");
    const [editLastName, setEditLastName] = useState("");
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    useEffect(() => {
        getUserProfile()
            .then((data) => {
                setProfile(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                const errMsg = err instanceof Error ? err.message : String(err);
                setError(errMsg || "Failed to load profile details.");
                setLoading(false);
            });
    }, []);

    const handleStartEdit = () => {
        setEditFirstName(profile?.firstName || "");
        setEditLastName(profile?.lastName || "");
        setSaveError(null);
        setIsEditing(true);
    };

    const handleCancel = () => {
        setIsEditing(false);
        setSaveError(null);
    };

    const handleSave = async () => {
        if (!editFirstName.trim() || !editLastName.trim()) {
            setSaveError("First Name and Last Name are required.");
            return;
        }

        setSaving(true);
        setSaveError(null);

        try {
            const updated = await updateUserProfile(editFirstName.trim(), editLastName.trim());
            setProfile(updated);
            setIsEditing(false);
        } catch (err) {
            console.error(err);
            const errMsg = err instanceof Error ? err.message : "Failed to update profile details.";
            setSaveError(errMsg);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="p-6 max-w-7xl mx-auto text-[#1A1A1A]">
                <div className="animate-pulse space-y-6">
                    <div className="space-y-2">
                        <div className="h-8 bg-[#F7F7F7] rounded w-1/4"></div>
                        <div className="h-4 bg-[#F7F7F7] rounded w-1/3"></div>
                    </div>
                    <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm h-64"></div>
                </div>
            </div>
        );
    }

    if (error || !profile) {
        return (
            <div className="p-6 max-w-7xl mx-auto text-[#1A1A1A]">
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 text-center">
                    <p className="text-red-600 font-semibold mb-4">Error: {error || "Profile could not be found."}</p>
                    <button
                        onClick={() => window.location.reload()}
                        className="bg-[#42211D] text-[#FFFFFF] font-bold py-2 px-4 rounded-lg hover:bg-[#2a1412] transition-colors"
                    >
                        Try Again
                    </button>
                </div>
            </div>
        );
    }

    const userRole = profile.role || "GUEST";

    const getRoleTitle = (role: string) => {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return "Admin Profile";
            case "OWNER":
                return "Owner Profile";
            case "GUEST":
            default:
                return "Guest Profile";
        }
    };

    const getRoleDescription = (role: string) => {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return "Manage system configuration and administrative accounts.";
            case "OWNER":
                return "Manage your property owner details and notification preferences.";
            case "GUEST":
            default:
                return "Manage your guest account details and notification preferences.";
        }
    };

    const getRoleColor = (role: string) => {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return "bg-blue-100 text-blue-700";
            case "OWNER":
                return "bg-amber-100 text-amber-700";
            case "GUEST":
            default:
                return "bg-emerald-100 text-emerald-700";
        }
    };

    const getRoleIcon = (role: string) => {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return (
                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                );
            case "OWNER":
                return (
                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                    </svg>
                );
            case "GUEST":
            default:
                return (
                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                );
        }
    };

    return (
        <div className="p-4 md:p-8 max-w-7xl mx-auto min-h-screen text-brand-main space-y-6">
            <div className="border-b border-brand-accent pb-4">
                <h1 className="text-3xl font-bold text-brand-main tracking-tight">{getRoleTitle(userRole)}</h1>
                <p className="text-sm text-brand-muted mt-1">{getRoleDescription(userRole)}</p>
            </div>

            <div className="bg-white border border-brand-accent rounded-xl shadow-sm hover:shadow-md transition-all duration-300">
                <div className="p-4 sm:p-8 border-b border-brand-accent">
                    <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                        <div className="flex items-center gap-6">
                            <div className={`w-20 h-20 rounded-full flex items-center justify-center shadow-md border border-brand-accent/50 ${getRoleColor(userRole)}`}>
                                {getRoleIcon(userRole)}
                            </div>
                            <div className="space-y-1">
                                <h2 className="text-2xl font-bold text-brand-main tracking-tight">
                                    {profile.firstName ? `${profile.firstName} ${profile.lastName}` : profile.username}
                                </h2>
                                <p className="text-sm text-brand-muted font-medium">Personal Account Details</p>
                            </div>
                        </div>
                        <div>
                            <span className="bg-brand-bg border border-brand-accent text-brand-primary font-bold text-xs uppercase tracking-wider px-3.5 py-2 rounded-full shadow-sm">
                                {userRole}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="p-4 sm:p-8">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        <div className="bg-brand-bg border border-brand-accent rounded-xl p-6 space-y-6">
                            <h3 className="text-sm font-bold text-brand-primary border-b border-brand-accent/60 pb-2">Basic Information</h3>
                            <div className="space-y-4">
                                <div className="space-y-1.5">
                                    <label className="block text-sm font-medium text-brand-muted mb-1">First Name</label>
                                    {isEditing ? (
                                        <input
                                            type="text"
                                            value={editFirstName}
                                            onChange={(e) => setEditFirstName(e.target.value)}
                                            className="w-full p-2.5 border border-brand-accent rounded-lg focus:outline-none focus:ring-1 focus:ring-brand-primary text-brand-main font-semibold bg-white shadow-sm transition-all"
                                            placeholder="Enter first name"
                                            required
                                        />
                                    ) : (
                                        <p className="text-base font-medium text-brand-main">{profile.firstName || "—"}</p>
                                    )}
                                </div>
                                <div className="space-y-1.5">
                                    <label className="block text-sm font-medium text-brand-muted mb-1">Last Name</label>
                                    {isEditing ? (
                                        <input
                                            type="text"
                                            value={editLastName}
                                            onChange={(e) => setEditLastName(e.target.value)}
                                            className="w-full p-2.5 border border-brand-accent rounded-lg focus:outline-none focus:ring-1 focus:ring-brand-primary text-brand-main font-semibold bg-white shadow-sm transition-all"
                                            placeholder="Enter last name"
                                            required
                                        />
                                    ) : (
                                        <p className="text-base font-medium text-brand-main">{profile.lastName || "—"}</p>
                                    )}
                                </div>
                                <div className="space-y-1.5">
                                    <label className="block text-sm font-medium text-brand-muted mb-1">Username</label>
                                    <p className="text-base font-medium text-brand-main">
                                        {profile.username}
                                    </p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-brand-bg border border-brand-accent rounded-xl p-6 space-y-6">
                            <h3 className="text-sm font-bold text-brand-primary border-b border-brand-accent/60 pb-2">Contact & Security</h3>
                            <div className="space-y-4">
                                <div className="space-y-1.5">
                                    <label className="block text-sm font-medium text-brand-muted mb-1">Email Address</label>
                                    <p className="text-base font-medium text-brand-main mb-2">
                                        {profile.email}
                                    </p>
                                    <span className="inline-flex items-center gap-1.5 text-xs text-green-700 font-bold bg-green-50 border border-green-200 px-3 py-1 rounded-full shadow-sm">
                                        <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        Active for Notifications
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {saveError && (
                        <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm mt-6">
                            <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                            </svg>
                            <span className="text-sm font-semibold">{saveError}</span>
                        </div>
                    )}

                    <div className="mt-8 flex justify-end gap-4 border-t border-brand-accent pt-6">
                        {isEditing ? (
                            <>
                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    disabled={saving}
                                    className="px-6 py-2.5 text-sm font-bold text-brand-primary bg-brand-bg border border-brand-accent hover:bg-gray-100 rounded-lg transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    onClick={handleSave}
                                    disabled={saving}
                                    className="px-6 py-2.5 bg-brand-primary text-white font-bold hover:bg-brand-primary-hover text-sm rounded-lg transition-colors border border-brand-accent shadow-sm disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
                                >
                                    {saving ? "Saving..." : "Save Changes"}
                                </button>
                            </>
                        ) : (
                            <button
                                type="button"
                                onClick={handleStartEdit}
                                className="px-6 py-2.5 bg-brand-primary text-white font-bold hover:bg-brand-primary-hover text-sm rounded-lg transition-colors border border-brand-accent shadow-sm disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
                            >
                                Edit Profile
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
