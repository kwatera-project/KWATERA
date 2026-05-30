const EXPLORE_ITEMS = [
  {
    title: 'Azure Haven',
    image: 'https://images.unsplash.com/photo-1454496522488-7a8e488e8606?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80'
  },
  {
    title: 'Emerald Valley',
    image: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80'
  },
  {
    title: 'Golden Peaks',
    image: 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80'
  }
];

export default function ExploreSection() {
  return (
    <section className="py-32 px-4 md:px-8 lg:px-16 bg-section border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto flex flex-col lg:flex-row gap-16 items-center">
        
        <div className="flex-1 w-full text-center lg:text-left">
          <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>Discover</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title mb-8 leading-tight">Explore Poland</h2>
          <p className="text-details text-lg mb-10 leading-relaxed max-w-xl mx-auto lg:mx-0">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla vitae felis vel sem pretium interdum vel eget ex.
          </p>
          <button className="bg-[rgb(var(--color-burgundy))] text-white font-bold py-4 px-10 rounded-full hover:bg-[rgb(var(--color-burgundy-hover))] transition-colors shadow-lg text-lg">
            See all destinations
          </button>
        </div>

        
        <div className="flex-1 w-full grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-6">
             <div className="w-full h-64 rounded-3xl overflow-hidden shadow-lg relative group">
                <img src={EXPLORE_ITEMS[0].image} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" alt="Explore 1"/>
                <div className="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors" />
             </div>
             <div className="w-full h-48 rounded-3xl overflow-hidden shadow-lg relative group">
                <img src={EXPLORE_ITEMS[1].image} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" alt="Explore 2"/>
                <div className="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors" />
             </div>
          </div>
          <div className="pt-0 md:pt-12">
             <div className="w-full h-80 md:h-96 rounded-3xl overflow-hidden shadow-lg relative group">
                <img src={EXPLORE_ITEMS[2].image} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" alt="Explore 3"/>
                <div className="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors" />
             </div>
          </div>
        </div>
      </div>
    </section>
  );
}
