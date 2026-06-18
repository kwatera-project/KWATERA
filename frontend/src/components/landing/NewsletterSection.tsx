export default function NewsletterSection() {
  return (
    <section className="py-24 px-4 md:px-8 lg:px-16 bg-stone-50/60 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="bg-gradient-to-br from-[#FAF5F2] via-[#F6E5E0] to-[#ECD5CE] rounded-[40px] overflow-hidden shadow-2xl flex flex-col lg:flex-row min-h-[450px]">

          <div className="w-full lg:w-5/12 h-72 lg:h-auto">
            <img 
              src="https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" 
              alt="Camping landscape" 
              className="w-full h-full object-cover"
            />
          </div>
          

          <div className="w-full lg:w-7/12 p-8 md:p-12 lg:p-20 flex flex-col justify-center">
            <span className="block text-xs md:text-sm font-bold uppercase tracking-widest mb-6" style={{ color: 'rgb(var(--color-burgundy))' }}>NEWSLETTER</span>
            <h2 className="text-3xl md:text-5xl font-bold text-title mb-6 leading-tight">
              Get special offers, and more from Kwatera
            </h2>
            <p className="text-details mb-10 text-base md:text-xl font-medium leading-relaxed">
              Join our community and be the first to know about exclusive discounts, personalized recommendations, and inspiring travel destinations directly in your inbox.
            </p>
            
            <form className="relative max-w-lg w-full flex flex-col sm:block gap-3" onSubmit={(e) => e.preventDefault()}>
              <input 
                type="email" 
                placeholder="Enter your email address" 
                className="w-full bg-card border border-[#DACDCA] text-title rounded-full py-4 sm:py-5 pl-6 sm:pl-8 pr-6 sm:pr-40 focus:outline-none focus:ring-2 focus:ring-[rgb(var(--color-burgundy))] focus:border-transparent transition-all shadow-sm font-medium"
                required
              />
              <button 
                type="submit" 
                className="w-full sm:w-auto mt-2 sm:mt-0 sm:absolute sm:right-2 sm:top-2 sm:bottom-2 bg-[rgb(var(--color-burgundy))] text-white font-bold text-base sm:text-lg rounded-full py-3 sm:py-0 px-8 hover:bg-[rgb(var(--color-burgundy-hover))] transition-colors shadow-md cursor-pointer"
              >
                Subscribe
              </button>
            </form>
          </div>
        </div>
      </div>
    </section>
  );
}
