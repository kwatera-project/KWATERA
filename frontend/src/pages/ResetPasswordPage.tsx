import { useState } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import toast from "react-hot-toast";
import { resetPassword } from "../api/authApi";

export default function ResetPasswordPage() {
    const { t } = useTranslation();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get("token") || "";

    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (password.length < 8) {
            toast.error(t("resetPassword.passwordLengthError"));
            return;
        }

        if (password !== confirmPassword) {
            toast.error(t("resetPassword.validationError"));
            return;
        }

        setLoading(true);
        try {
            await resetPassword(token, password);
            toast.success(t("resetPassword.successMessage"));
            navigate("/login");
        } catch {
            toast.error(t("resetPassword.errorMessage"));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="h-screen w-full flex flex-col bg-white">
            <header className="flex-none bg-white border-b border-gray-100 px-8 py-9 flex items-center justify-between z-50 shadow-sm">
            </header>

            <div className="flex-1 w-full grid grid-cols-1 lg:grid-cols-12 overflow-hidden">
                <div className="lg:col-span-5 flex flex-col justify-center items-center overflow-y-auto p-4">
                    <div className="w-full max-w-md bg-white border border-gray-100 rounded-2xl shadow-xl p-8 sm:p-10 space-y-6">
                        <div className="text-center space-y-2">
                            <h1 className="text-2xl font-bold text-gray-900">{t("resetPassword.title")}</h1>
                            <p className="text-sm text-gray-500">{t("resetPassword.subtitle")}</p>
                        </div>

                        {!token ? (
                            <div className="space-y-4">
                                <div className="p-4 rounded-lg text-sm font-medium bg-red-50 text-red-700 text-center">
                                    {t("resetPassword.errorMessage")}
                                </div>
                                <div className="text-center">
                                    <Link to="/login" className="text-[#42211D] font-bold hover:underline">
                                        {t("forgotPassword.backToLogin")}
                                    </Link>
                                </div>
                            </div>
                        ) : (
                            <form onSubmit={handleSubmit} className="space-y-4">
                                <div className="space-y-1.5">
                                    <label className="block text-sm font-medium text-gray-700">{t("resetPassword.newPassword")}</label>
                                    <input
                                        type="password"
                                        placeholder="••••••••"
                                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] focus:border-[#42211D] outline-none transition"
                                        value={password}
                                        onChange={e => setPassword(e.target.value)}
                                        required
                                        disabled={loading}
                                    />
                                </div>

                                <div className="space-y-1.5">
                                    <label className="block text-sm font-medium text-gray-700">{t("resetPassword.confirmPassword")}</label>
                                    <input
                                        type="password"
                                        placeholder="••••••••"
                                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] focus:border-[#42211D] outline-none transition"
                                        value={confirmPassword}
                                        onChange={e => setConfirmPassword(e.target.value)}
                                        required
                                        disabled={loading}
                                    />
                                </div>

                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="w-full py-3 bg-[#42211D] text-white font-bold rounded-lg hover:opacity-90 transition shadow-sm disabled:opacity-50"
                                >
                                    {t("resetPassword.submit")}
                                </button>
                            </form>
                        )}
                    </div>
                </div>

                <div className="hidden lg:block lg:col-span-7 relative overflow-hidden">
                    <img
                        src="https://images.pexels.com/photos/5364965/pexels-photo-5364965.jpeg"
                        alt={t("login.imageAlt")}
                        className="w-full h-full object-cover"
                    />
                </div>
            </div>
        </div>
    );
}
