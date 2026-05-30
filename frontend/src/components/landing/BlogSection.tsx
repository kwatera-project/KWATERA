import { Calendar, ArrowRightCircle } from 'lucide-react';

const BLOG_POSTS = [
  {
    id: 1,
    title: 'Top 10 Hidden Gems in Tatra Mountains',
    date: 'February 20, 2024',
    excerpt: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore.',
    image: 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 2,
    title: 'A Guide to Polish Cuisine for Travelers',
    date: 'March 05, 2024',
    excerpt: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
    image: 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 3,
    title: 'Sustainable Tourism: How to Travel Green',
    date: 'April 12, 2024',
    excerpt: 'Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
    image: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80'
  }
];

export default function BlogSection() {
  return (
    <section className="bg-card py-32 px-4 md:px-8 lg:px-16 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="mb-16">
          <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>Our Blog</span>
          <h2 className="text-3xl md:text-5xl font-bold text-title mb-3">Travel Tips & Guides</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 mb-16">
          {BLOG_POSTS.map((post) => (
            <article key={post.id} className="group flex flex-col cursor-pointer bg-white rounded-3xl overflow-hidden shadow-lg border border-gray-100 pb-8 hover:shadow-xl transition-shadow">
              <div className="relative aspect-[4/3] w-full overflow-hidden mb-6">
                <img 
                  src={post.image} 
                  alt={post.title} 
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700"
                />
              </div>
              
              <div className="px-8 flex-grow flex flex-col">
                  <div className="flex items-center gap-2 text-sm font-bold mb-4" style={{ color: 'rgb(var(--color-burgundy))' }}>
                    <Calendar size={18} />
                    <span>{post.date}</span>
                  </div>
                  
                  <h3 className="text-2xl font-bold text-title mb-4 hover:text-[rgb(var(--color-burgundy))] transition-colors leading-snug">
                    {post.title}
                  </h3>
                  
                  <p className="text-details mb-8 flex-grow line-clamp-3 text-lg">
                    {post.excerpt}
                  </p>
                  
                  <div className="flex items-center gap-2 font-bold mt-auto group-hover:gap-4 transition-all text-lg" style={{ color: 'rgb(var(--color-burgundy))' }}>
                    <span>See more</span>
                    <ArrowRightCircle size={24} />
                  </div>
              </div>
            </article>
          ))}
        </div>



      </div>
    </section>
  );
}
