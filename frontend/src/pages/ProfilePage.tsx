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
        <div className="p-6 max-w-7xl mx-auto text-[#1A1A1A]">
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-[#1A1A1A]">{getRoleTitle(userRole)}</h1>
                <p className="text-sm text-[#7A7A7A]">{getRoleDescription(userRole)}</p>
            </div>

            <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm">
                <div className="p-8 border-b border-[#DACDCA] mb-6">
                    <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                        <div className="flex items-center gap-6">
                            <div className={`w-20 h-20 rounded-full flex items-center justify-center shadow-md ${getRoleColor(userRole)}`}>
                                {getRoleIcon(userRole)}
                            </div>
                            <div>
                                <h2 className="text-2xl font-bold text-[#1A1A1A]">
                                    {profile.firstName ? `${profile.firstName} ${profile.lastName}` : profile.username}
                                </h2>
                                <p className="text-[#7A7A7A] mt-1">Personal Account Details</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <span className="bg-[#F7F7F7] border border-[#DACDCA] text-[#42211D] font-bold text-xs uppercase tracking-wider px-3 py-1.5 rounded-full shadow-sm">
                                {userRole}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="px-8 pb-8">

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-6">
                            <h3 className="text-sm font-bold text-[#7A7A7A] uppercase tracking-wider mb-4">Basic Information</h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-xs font-semibold text-[#7A7A7A] mb-1">First Name</label>
                                    {isEditing ? (
                                        <input
                                            type="text"
                                            value={editFirstName}
                                            onChange={(e) => setEditFirstName(e.target.value)}
                                            className="w-full p-2 border border-[#DACDCA] rounded-lg focus:outline-none focus:ring-1 focus:ring-[#42211D] text-[#1A1A1A] font-semibold bg-white"
                                            placeholder="Enter first name"
                                            required
                                        />
                                    ) : (
                                        <p className="font-semibold text-[#1A1A1A]">{profile.firstName || "—"}</p>
                                    )}
                                </div>
                                <div>
                                    <label className="block text-xs font-semibold text-[#7A7A7A] mb-1">Last Name</label>
                                    {isEditing ? (
                                        <input
                                            type="text"
                                            value={editLastName}
                                            onChange={(e) => setEditLastName(e.target.value)}
                                            className="w-full p-2 border border-[#DACDCA] rounded-lg focus:outline-none focus:ring-1 focus:ring-[#42211D] text-[#1A1A1A] font-semibold bg-white"
                                            placeholder="Enter last name"
                                            required
                                        />
                                    ) : (
                                        <p className="font-semibold text-[#1A1A1A]">{profile.lastName || "—"}</p>
                                    )}
                                </div>
                                <div>
                                    <label className="block text-xs font-semibold text-[#7A7A7A] mb-1">Username</label>
                                    <p className="font-semibold text-[#7A7A7A] bg-gray-100 p-2 rounded-lg border border-gray-200 select-none">
                                        {profile.username}
                                    </p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-6">
                            <h3 className="text-sm font-bold text-[#7A7A7A] uppercase tracking-wider mb-4">Contact & Security</h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-xs font-semibold text-[#7A7A7A] mb-1">Email Address</label>
                                    <p className="font-semibold text-[#7A7A7A] bg-gray-100 p-2 rounded-lg border border-gray-200 select-none">
                                        {profile.email}
                                    </p>
                                    <span className="inline-flex items-center gap-1.5 text-xs text-green-700 font-medium mt-2">
                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        Active for notifications
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {saveError && (
                        <div className="mt-6 bg-red-50 border border-red-200 rounded-lg p-3 text-red-700 text-sm font-semibold">
                            {saveError}
                        </div>
                    )}

                    <div className="mt-8 flex justify-end gap-4">
                        {isEditing ? (
                            <>
                                <button
                                    type="button"
                                    onClick={handleCancel}
                                    disabled={saving}
                                    className="bg-[#F7F7F7] text-[#1A1A1A] border border-[#DACDCA] font-bold py-2.5 px-6 rounded-lg hover:bg-gray-100 transition-colors shadow-sm disabled:opacity-50"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    onClick={handleSave}
                                    disabled={saving}
                                    className="bg-[#42211D] text-[#FFFFFF] font-bold py-2.5 px-6 rounded-lg hover:bg-[#2a1412] transition-colors shadow-sm disabled:opacity-50 flex items-center gap-2"
                                >
                                    {saving ? "Saving..." : "Save Changes"}
                                </button>
                            </>
                        ) : (
                            <button
                                type="button"
                                onClick={handleStartEdit}
                                className="bg-[#42211D] text-[#FFFFFF] font-bold py-2.5 px-6 rounded-lg hover:bg-[#2a1412] transition-colors shadow-sm"
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
