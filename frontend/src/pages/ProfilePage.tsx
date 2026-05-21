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

    return (
        <div className="p-6 max-w-7xl mx-auto text-[#1A1A1A]">
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-[#1A1A1A]">{getRoleTitle(userRole)}</h1>
                <p className="text-sm text-[#7A7A7A]">{getRoleDescription(userRole)}</p>
            </div>

            <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm overflow-hidden">
                <div className="bg-[#42211D] h-24 relative">
                    <div className="absolute -bottom-10 left-8">
                        <div className="w-20 h-20 bg-white border border-[#DACDCA] rounded-full flex items-center justify-center shadow-md">
                            <span className="text-[#42211D] text-2xl font-extrabold select-none">
                                {profile.firstName ? profile.firstName.substring(0, 1).toUpperCase() : profile.username.substring(0, 1).toUpperCase()}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="pt-14 pb-8 px-8">
                    <div className="flex flex-col md:flex-row justify-between items-start md:items-center border-b border-[#DACDCA] pb-6 mb-6">
                        <div>
                            <h2 className="text-xl font-bold text-[#1A1A1A]">
                                {profile.firstName ? `${profile.firstName} ${profile.lastName}` : profile.username}
                            </h2>
                            <p className="text-sm text-[#7A7A7A] mt-1">Personal Account Details</p>
                        </div>
                        <div className="mt-4 md:mt-0 flex items-center gap-3">
                            <span className="bg-[#F7F7F7] border border-[#DACDCA] text-[#42211D] font-bold text-xs uppercase tracking-wider px-3 py-1.5 rounded-full shadow-sm">
                                {userRole}
                            </span>
                        </div>
                    </div>

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
                                <div>
                                    <label className="block text-xs font-semibold text-[#7A7A7A] mb-1">Account ID</label>
                                    <p className="font-mono text-xs text-[#7A7A7A] bg-gray-100 p-2 rounded-lg border border-gray-200 overflow-x-auto select-all">
                                        {profile.id}
                                    </p>
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
