import { Tag, ShieldCheck, Zap } from 'lucide-react';

const FEATURES = [
  {
    title: 'Competitive Prices',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    icon: <Tag size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
  },
  {
    title: 'Secure Booking',
    description: 'Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.',
    icon: <ShieldCheck size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
  },
  {
    title: 'Seamless Experience',
    description: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
    icon: <Zap size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
  }
];

export default function FeaturesSection() {
  return (
    <section className="bg-card py-32 px-4 md:px-8 lg:px-16">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>Why Choose Us?</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title">Experience the best of Kwatera</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 md:gap-16 mb-24">
          {FEATURES.map((feature, idx) => (
            <div key={idx} className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-3xl bg-main flex items-center justify-center mb-6 shadow-sm border border-[#DACDCA]">
                {feature.icon}
              </div>
              <h3 className="text-2xl font-bold text-title mb-4">{feature.title}</h3>
              <p className="text-details leading-relaxed text-lg">
                {feature.description}
              </p>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
