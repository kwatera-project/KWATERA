import { useTranslation } from "react-i18next";
export default function NewsletterSection() {
    const {t} = useTranslation();
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
            <span className="block text-xs md:text-sm font-bold uppercase tracking-widest mb-6" style={{ color: 'rgb(var(--color-burgundy))' }}>NEWSLETTER</span>
            <h2 className="text-3xl md:text-5xl font-bold text-title mb-6 leading-tight">
                {t("newsletterSection.title")}
            </h2>
            <p className="text-details mb-10 text-base md:text-xl font-medium leading-relaxed">
                {t("newsletterSection.description")}
            </p>
            
            <form className="relative max-w-lg w-full" onSubmit={(e) => e.preventDefault()}>
              <input 
                type="email"
                placeholder={t("newsletterSection.emailPlaceholder")}
                className="w-full bg-card border border-[#DACDCA] text-title rounded-full py-5 pl-8 pr-40 focus:outline-none focus:ring-2 focus:ring-[rgb(var(--color-burgundy))] focus:border-transparent transition-all shadow-sm font-medium"
                required
              />
              <button 
                type="submit" 
                className="absolute right-2 top-2 bottom-2 bg-[rgb(var(--color-burgundy))] text-white font-bold text-lg rounded-full px-8 hover:bg-[rgb(var(--color-burgundy-hover))] transition-colors shadow-md"
              >
                  {t("newsletterSection.subscribe")}
              </button>
            </form>
          </div>
        </div>
      </div>
    </section>
  );
}
