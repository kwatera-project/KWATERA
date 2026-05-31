import { Calendar, ArrowRightCircle } from 'lucide-react';

const BLOG_POSTS = [
  {
    id: 1,
    title: 'Top 10 Hidden Gems in Tatra Mountains',
    date: 'February 20, 2026',
    excerpt: 'Escape the crowds and discover breathtaking, untamed trails. Here is our exclusive list of the most peaceful spots in the Tatras.',
    image: 'https://images.pexels.com/photos/34804821/pexels-photo-34804821.jpeg'
  },
  {
    id: 2,
    title: 'A Guide to Polish Cuisine for Travelers',
    date: 'March 05, 2025',
    excerpt: 'From classic pierogi to hearty regional stews. Explore the must-try dishes that will make your Polish journey unforgettable.',
    image: 'https://images.pexels.com/photos/19969456/pexels-photo-19969456.jpeg'
  },
  {
    id: 3,
    title: 'Sustainable Tourism: How to Travel Green',
    date: 'April 12, 2025',
    excerpt: 'Learn how to explore the world responsibly. Discover simple, actionable tips to reduce your carbon footprint while enjoying your vacation.',
    image: 'https://images.pexels.com/photos/29150081/pexels-photo-29150081.jpeg'
  }
];

export default function BlogSection() {
  return (
    <section className="bg-white py-24 px-4 md:px-8 lg:px-16 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="mb-16">
          <span className="block text-xs md:text-sm font-bold uppercase tracking-widest mb-2.5" style={{ color: 'rgb(var(--color-burgundy))' }}>Our Blog</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title mb-3">Travel Tips & Guides</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 mb-16">
          {BLOG_POSTS.map((post) => (
            <article key={post.id} className="group flex flex-col bg-white rounded-[32px] overflow-hidden shadow-md border border-gray-100 pb-8 transition-all duration-300 hover:-translate-y-1.5 hover:shadow-xl cursor-pointer">
              <div className="relative aspect-[4/3] w-full overflow-hidden mb-6">
                <img 
                  src={post.image} 
                  alt={post.title} 
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700"
                />
              </div>
              
              <div className="px-8 flex-grow flex flex-col">
                  <div className="flex items-center gap-1.5 text-xs md:text-sm font-bold mb-3.5" style={{ color: 'rgb(var(--color-burgundy))' }}>
                    <Calendar size={15} className="inline-block align-middle" />
                    <span className="leading-none mt-[1px]">{post.date}</span>
                  </div>
                  
                  <h3 className="text-xl md:text-2xl font-bold text-title mb-4 group-hover:text-[rgb(var(--color-burgundy))] transition-colors leading-snug">
                    {post.title}
                  </h3>
                  
                  <p className="text-details mb-8 flex-grow line-clamp-3 text-base md:text-lg">
                    {post.excerpt}
                  </p>
                  
                  <div className="flex items-center gap-2 font-bold mt-auto group-hover:text-[rgb(var(--color-burgundy-hover))] transition-colors text-base md:text-lg" style={{ color: 'rgb(var(--color-burgundy))' }}>
                    <span>See more</span>
                    <ArrowRightCircle size={22} className="group-hover:translate-x-1 transition-transform" />
                  </div>
              </div>
            </article>
          ))}
        </div>



      </div>
    </section>
  );
}
