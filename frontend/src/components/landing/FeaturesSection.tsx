import { Tag, ShieldCheck, Zap } from 'lucide-react';
import { useTranslation } from "react-i18next";

const FEATURES = [
    {
        titleKey: "featuresSection.feature1Title",
        descriptionKey: "featuresSection.feature1Description",
        icon: <Tag size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
    },
    {
        titleKey: "featuresSection.feature2Title",
        descriptionKey: "featuresSection.feature2Description",
        icon: <ShieldCheck size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
    },
    {
        titleKey: "featuresSection.feature3Title",
        descriptionKey: "featuresSection.feature3Description",
        icon: <Zap size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
    }
];

export default function FeaturesSection() {
    const {t} = useTranslation();
  return (
    <section className="bg-stone-50/60 py-24 px-4 md:px-8 lg:px-16">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>{t("featuresSection.badge")}</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title">{t("featuresSection.title")}</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 md:gap-16 mb-24">
          {FEATURES.map((feature, idx) => (
            <div key={idx} className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-3xl bg-main flex items-center justify-center mb-8 shadow-sm border border-[#DACDCA]">
                {feature.icon}
              </div>
              <h3 className="text-2xl font-bold text-title mb-5">{t(feature.titleKey)}</h3>
              <p className="text-details leading-relaxed text-lg">
                  {t(feature.descriptionKey)}
              </p>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
