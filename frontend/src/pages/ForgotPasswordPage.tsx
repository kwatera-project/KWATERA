import { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import toast from "react-hot-toast";
import { forgotPassword } from "../api/authApi";

export default function ForgotPasswordPage() {
    const { t } = useTranslation();
    const [email, setEmail] = useState("");
    const [loading, setLoading] = useState(false);
    const [submitted, setSubmitted] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            await forgotPassword(email);
            toast.success(t("forgotPassword.successMessage"));
            setSubmitted(true);
        } catch {
            toast.error(t("forgotPassword.errorMessage"));
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
                            <h1 className="text-2xl font-bold text-gray-900">{t("forgotPassword.title")}</h1>
                            <p className="text-sm text-gray-500">{t("forgotPassword.subtitle")}</p>
                        </div>

                        {submitted ? (
                            <div className="space-y-4">
                                <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm font-medium">
                                    {t("forgotPassword.successMessage")}
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
                                    <label className="block text-sm font-medium text-gray-700">{t("forgotPassword.emailLabel")}</label>
                                    <input
                                        type="email"
                                        placeholder={t("forgotPassword.emailPlaceholder")}
                                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] focus:border-[#42211D] outline-none transition"
                                        value={email}
                                        onChange={e => setEmail(e.target.value)}
                                        required
                                        disabled={loading}
                                    />
                                </div>

                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="w-full py-3 bg-[#42211D] text-white font-bold rounded-lg hover:opacity-90 transition shadow-sm disabled:opacity-50"
                                >
                                    {t("forgotPassword.submit")}
                                </button>

                                <div className="text-center">
                                    <Link to="/login" className="text-[#42211D] font-bold hover:underline">
                                        {t("forgotPassword.backToLogin")}
                                    </Link>
                                </div>
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
