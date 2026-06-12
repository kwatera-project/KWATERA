import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="bg-[#4E2723] text-white pt-24 pb-12 px-4 md:px-12 lg:px-20 w-full mt-10 transition-colors duration-300">
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-16 lg:gap-24 mb-16 text-left">

          <div className="text-left">
            <h3 className="font-bold text-xl mb-6 tracking-wide text-white text-left">Support</h3>
            <ul className="space-y-4 text-white/80 text-lg text-left">
              <li className="text-left"><a href="#" className="hover:text-white transition-colors block text-left">Help Center</a></li>
              <li className="text-left"><a href="#" className="hover:text-white transition-colors block text-left">Report an issue</a></li>
              <li className="text-left"><a href="#" className="hover:text-white transition-colors block text-left">Privacy Terms</a></li>
            </ul>
          </div>
          

          <div className="text-left">
            <h3 className="font-bold text-xl mb-6 tracking-wide text-white text-left">Company</h3>
            <ul className="space-y-4 text-white/80 text-lg text-left">
              <li className="text-left"><Link to="/about" className="hover:text-white transition-colors block text-left">About us</Link></li>
              <li className="text-left"><a href="#" className="hover:text-white transition-colors block text-left">Press room</a></li>
            </ul>
          </div>
          

          <div className="text-left">
            <h3 className="font-bold text-xl mb-6 tracking-wide text-white text-left">Contact</h3>
            <ul className="space-y-4 text-white/80 text-lg text-left">
              <li className="text-left"><a href="#" className="hover:text-white transition-colors block text-left">FAQ</a></li>
              <li className="text-left"><a href="#" className="hover:text-white transition-colors block text-left">Support center</a></li>
            </ul>
          </div>
          

          <div>
            <h3 className="font-bold text-xl mb-6 tracking-wide text-white">Social</h3>
            <div className="flex gap-4">
              <a href="#" className="w-12 h-12 rounded-full border border-white/30 flex items-center justify-center hover:bg-white hover:text-[rgb(var(--color-burgundy))] transition-all text-white">
                <span className="sr-only">Facebook</span>
                <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24"><path d="M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z"></path></svg>
              </a>
              <a href="#" className="w-12 h-12 rounded-full border border-white/30 flex items-center justify-center hover:bg-white hover:text-[rgb(var(--color-burgundy))] transition-all text-white">
                <span className="sr-only">Twitter</span>
                <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24"><path d="M23 3a10.9 10.9 0 01-3.14 1.53 4.48 4.48 0 00-7.86 3v1A10.66 10.66 0 013 4s-4 9 5 13a11.64 11.64 0 01-7 2c9 5 20 0 20-11.5a4.5 4.5 0 00-.08-.83A7.72 7.72 0 0023 3z"></path></svg>
              </a>
              <a href="#" className="w-12 h-12 rounded-full border border-white/30 flex items-center justify-center hover:bg-white hover:text-[rgb(var(--color-burgundy))] transition-all text-white">
                <span className="sr-only">TikTok</span>
                <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24"><path d="M12.53.02C13.84 0 15.14.01 16.44 0c.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.62 4.24 1.79v4.03c-1.44-.05-2.89-.35-4.2-.97-.57-.26-1.1-.59-1.62-.93v7.2c0 1.63-.3 3.32-1.22 4.71-1.39 2.08-3.9 3.34-6.36 3.03-3.14-.38-5.83-2.9-6.3-6.05-.33-2.22.42-4.57 2.05-6.07 1.55-1.44 3.73-2.06 5.8-1.74v4.06c-1.22-.07-2.5.2-3.41 1.02-.91.82-1.36 2.07-1.15 3.31.25 1.48 1.52 2.75 3.03 2.97 1.49.21 3.08-.34 3.97-1.48.91-1.15 1.25-2.65 1.25-4.14V.02zm-7.07 15.71c.07-.46.36-.88.75-1.1.41-.24.91-.32 1.38-.25.46.07.88.36 1.1.75.24.41.32.91.25 1.38-.07.46-.36.88-.75 1.1-.41.24-.91.32-1.38.25-.46-.07-.88-.36-1.1-.75-.24-.41-.32-.91-.25-1.38z"></path></svg>
              </a>
              <a href="#" className="w-12 h-12 rounded-full border border-white/30 flex items-center justify-center hover:bg-white hover:text-[rgb(var(--color-burgundy))] transition-all text-white">
                <span className="sr-only">YouTube</span>
                <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24"><path d="M22.54 6.42a2.78 2.78 0 00-1.94-1.96C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 00-1.94 1.96A29.33 29.33 0 001 11.75a29.4 29.4 0 00.46 5.33 2.78 2.78 0 001.94 1.96c1.72.46 8.6.46 8.6.46s6.88 0 8.6-.46a2.78 2.78 0 001.94-1.96 29.33 29.33 0 00.46-5.33 29.4 29.4 0 00-.46-5.33zM9.75 15.02V8.48l6.5 3.27-6.5 3.27z"></path></svg>
              </a>
            </div>
          </div>
        </div>
        
        <div className="border-t border-white/20 pt-10 flex flex-col md:flex-row justify-between items-center text-white/60 text-base">
          <p>&copy; {new Date().getFullYear()} KWATERA. All rights reserved.</p>
          <div className="flex gap-8 mt-6 md:mt-0">
            <a href="#" className="hover:text-white transition-colors">Privacy Policy</a>
            <a href="#" className="hover:text-white transition-colors">Terms of Service</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
