import { useState, useEffect, FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { subscribeToNewsletter } from "../../api/newsletterApi";
import toast from "react-hot-toast";

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function NewsletterSection() {
  const { t } = useTranslation();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("newsletterConfirmed") === "true") {
      toast.success("Your subscription has been confirmed!");
      const newUrl = window.location.pathname;
      window.history.replaceState({}, document.title, newUrl);
    }
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!emailRegex.test(email)) {
      toast.error("Please enter a valid email address.");
      return;
    }
    setLoading(true);
    try {
      await subscribeToNewsletter(email);
      toast.success("Please check your email to confirm subscription.");
      setEmail("");
    } catch (err: any) {
      toast.error(err.message || "Something went wrong.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="py-24 px-4 md:px-8 lg:px-16 bg-stone-50/60 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="bg-gradient-to-br from-[#FAF5F2] via-[#F6E5E0] to-[#ECD5CE] rounded-[40px] overflow-hidden shadow-2xl flex flex-col lg:flex-row min-h-[450px]">
          <div className="w-full lg:w-5/12 h-72 lg:h-auto">
            <img 
              src="https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
              alt={t("newsletterSection.imageAlt")}
              className="w-full h-full object-cover"
            />
          </div>
          <div className="w-full lg:w-7/12 p-8 md:p-12 lg:p-20 flex flex-col justify-center">
            <span className="block text-xs md:text-sm font-bold uppercase tracking-widest mb-6" style={{ color: 'rgb(var(--color-burgundy))' }}>{t("newsletterSection.badge")}</span>
            <h2 className="text-3xl md:text-5xl font-bold text-title mb-6 leading-tight">
              {t("newsletterSection.title")}
            </h2>
            <p className="text-details mb-10 text-base md:text-xl font-medium leading-relaxed">
              {t("newsletterSection.description")}
            </p>
            <form className="relative max-w-lg w-full flex flex-col sm:block gap-3" onSubmit={handleSubmit}>
              <input 
                type="email" 
                placeholder={t("newsletterSection.emailPlaceholder")}
                className="w-full bg-card border border-[#DACDCA] text-title rounded-full py-4 sm:py-5 pl-6 sm:pl-8 pr-6 sm:pr-40 focus:outline-none focus:ring-2 focus:ring-[rgb(var(--color-burgundy))] focus:border-transparent transition-all shadow-sm font-medium"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={loading}
                required
              />
              <button 
                type="submit" 
                className="w-full sm:w-auto mt-2 sm:mt-0 sm:absolute sm:right-2 sm:top-2 sm:bottom-2 bg-[rgb(var(--color-burgundy))] text-white font-bold text-base sm:text-lg rounded-full py-3 sm:py-0 px-8 hover:bg-[rgb(var(--color-burgundy-hover))] transition-colors shadow-md cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={loading}
              >
                {loading ? "Subscribing..." : t("newsletterSection.subscribe")}
              </button>
            </form>
          </div>
        </div>
      </div>
    </section>
  );
}
