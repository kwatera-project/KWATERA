import { Lock } from 'lucide-react';

export default function TrustBadges() {
  return (
    <div className="w-full bg-white py-10 border-t border-[#DACDCA]/20">
      <div className="max-w-7xl mx-auto px-4 flex flex-col md:flex-row gap-6 md:gap-8 justify-center items-center opacity-60 grayscale hover:opacity-90 transition-opacity duration-300">
        
        <div className="flex items-center gap-2 font-black text-xs md:text-sm tracking-widest text-title uppercase">
          <Lock size={15} className="text-orange-900" />
          <span>Secure Checkout</span>
        </div>

        <div className="h-4 w-px bg-stone-300 hidden md:block"></div>

        <div className="flex gap-6 md:gap-8 items-center">
          <span className="font-extrabold text-lg tracking-tighter text-blue-900 italic">VISA</span>

          <div className="flex items-center gap-1">
            <span className="font-bold text-xs tracking-tight text-title leading-none">mastercard</span>
            <div className="flex -space-x-2">
              <div className="w-4 h-4 rounded-full bg-red-500 opacity-80"></div>
              <div className="w-4 h-4 rounded-full bg-yellow-500 opacity-80"></div>
            </div>
          </div>

          <div className="flex items-center gap-1 border border-stone-300 px-2 py-0.5 rounded">
            <span className="font-black text-xs tracking-tighter text-stone-700 italic">blik</span>
          </div>

        </div>
      </div>
    </div>
  );
}
