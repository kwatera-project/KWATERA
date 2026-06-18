import { useTranslation } from "react-i18next";

const EXPLORE_ITEMS = [
  {
    title: 'Azure Haven',
    image: 'https://images.pexels.com/photos/34054370/pexels-photo-34054370.jpeg'
  },
  {
    title: 'Emerald Valley',
    image: 'https://images.pexels.com/photos/37115349/pexels-photo-37115349.jpeg'
  },
  {
    title: 'Golden Peaks',
    image: 'https://images.pexels.com/photos/32906975/pexels-photo-32906975.jpeg'
  }
];

export default function ExploreSection() {
    const {t} = useTranslation();
  return (
    <section className="bg-orange-50/20 py-24 px-4 md:px-8 lg:px-16 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto flex flex-col lg:flex-row gap-16 items-center">
        
        <div className="flex-1 w-full text-left flex flex-col items-start justify-center">
          <span className="block text-xs md:text-sm font-bold uppercase tracking-widest mb-2.5" style={{ color: 'rgb(var(--color-burgundy))' }}>{t("exploreSection.badge")}</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title mb-5 leading-tight">{t("exploreSection.title")}</h2>
          <p className="text-details text-lg mb-8 leading-relaxed max-w-xl font-medium">
              {t("exploreSection.description")}
          </p>
          <button className="bg-[rgb(var(--color-burgundy))] text-white font-semibold py-2.5 px-7 rounded-full hover:bg-[rgb(var(--color-burgundy-hover))] transition-all shadow-md text-sm hover:shadow-lg hover:-translate-y-0.5 active:translate-y-0 duration-200">
              {t("exploreSection.button")}
          </button>
        </div>

        
        <div className="flex-1 w-full grid grid-cols-1 md:grid-cols-2 gap-6 items-end">
          <div className="flex flex-col gap-6 w-full">
             <div className="w-full h-60 md:h-[240px] rounded-[32px] overflow-hidden shadow-lg relative group">
                <img src={EXPLORE_ITEMS[0].image} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" alt={t("exploreSection.image1Alt")}/>
                <div className="absolute inset-0 bg-black/15 group-hover:bg-black/10 transition-colors" />
             </div>
             <div className="w-full h-48 md:h-[180px] rounded-[32px] overflow-hidden shadow-lg relative group">
                <img src={EXPLORE_ITEMS[1].image} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" alt={t("exploreSection.image2Alt")}/>
                <div className="absolute inset-0 bg-black/15 group-hover:bg-black/10 transition-colors" />
             </div>
          </div>
          <div className="w-full pt-0 md:pt-12">
             <div className="w-full h-80 md:h-[396px] rounded-[32px] overflow-hidden shadow-lg relative group">
                <img src={EXPLORE_ITEMS[2].image} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" alt={t("exploreSection.image3Alt")}/>
                <div className="absolute inset-0 bg-black/15 group-hover:bg-black/10 transition-colors" />
             </div>
          </div>
        </div>
      </div>
    </section>
  );
}
