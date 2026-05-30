export default function NewsletterSection() {
  return (
    <section className="py-32 px-4 md:px-8 lg:px-16 bg-card border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="bg-main rounded-[40px] overflow-hidden shadow-2xl flex flex-col lg:flex-row min-h-[450px]">

          <div className="w-full lg:w-5/12 h-72 lg:h-auto">
            <img 
              src="https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" 
              alt="Camping landscape" 
              className="w-full h-full object-cover"
            />
          </div>
          

          <div className="w-full lg:w-7/12 p-8 md:p-12 lg:p-20 flex flex-col justify-center">
            <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>Newsletter</span>
            <h2 className="text-3xl md:text-5xl font-bold text-title mb-6 leading-tight">
              Get special offers, and more from travelworld
            </h2>
            <p className="text-details mb-12 text-xl font-medium">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum.
            </p>
            
            <form className="relative max-w-lg w-full" onSubmit={(e) => e.preventDefault()}>
              <input 
                type="email" 
                placeholder="Enter your email address" 
                className="w-full bg-card border border-[#DACDCA] text-title rounded-full py-5 pl-8 pr-40 focus:outline-none focus:ring-2 focus:ring-[rgb(var(--color-burgundy))] focus:border-transparent transition-all shadow-sm font-medium"
                required
              />
              <button 
                type="submit" 
                className="absolute right-2 top-2 bottom-2 bg-[rgb(var(--color-burgundy))] text-white font-bold text-lg rounded-full px-8 hover:bg-[rgb(var(--color-burgundy-hover))] transition-colors shadow-md"
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
