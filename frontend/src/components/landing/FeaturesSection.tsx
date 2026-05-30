import { Tag, ShieldCheck, Zap } from 'lucide-react';

const FEATURES = [
  {
    title: 'Competitive Prices',
    description: 'We offer the best rates on the market with no hidden fees. Find your dream accommodation that fits your budget perfectly.',
    icon: <Tag size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
  },
  {
    title: 'Secure Booking',
    description: 'Your personal data and payments are completely safe. We use state-of-the-art encryption to ensure a worry-free transaction.',
    icon: <ShieldCheck size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
  },
  {
    title: 'Seamless Experience',
    description: 'Booking your next trip has never been easier. Our intuitive platform allows you to find and reserve places in just a few clicks.',
    icon: <Zap size={28} style={{ color: 'rgb(var(--color-burgundy))' }} />
  }
];

export default function FeaturesSection() {
  return (
    <section className="bg-stone-50/60 py-24 px-4 md:px-8 lg:px-16">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>Why Choose Us?</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title">Experience the best of Kwatera</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 md:gap-16 mb-24">
          {FEATURES.map((feature, idx) => (
            <div key={idx} className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-3xl bg-main flex items-center justify-center mb-8 shadow-sm border border-[#DACDCA]">
                {feature.icon}
              </div>
              <h3 className="text-2xl font-bold text-title mb-5">{feature.title}</h3>
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
