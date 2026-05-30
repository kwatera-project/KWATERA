import '../App.css'
import { Link, useLocation } from "react-router-dom"
import { useLogout } from "./Logout.tsx"
import { getUserRoles, decodeJwt } from "../utils/jwtUtils.ts"
import { useState, useEffect, useRef } from "react"
import { useCurrency } from "../contexts/CurrencyContext"
import { User, LogOut, LayoutDashboard, Calendar, Settings } from 'lucide-react'

export default function Navbar() {
    const token = localStorage.getItem("token");
    const userRoles = getUserRoles(token);
    const isLoggedIn = !!token;
    const payload = token ? decodeJwt(token) : null;
    const sub = payload?.sub as string | undefined;
    const firstName = payload?.firstName as string | undefined;
    const lastName = payload?.lastName as string | undefined;
    const displayName = (firstName && lastName) ? `${firstName} ${lastName}` : (sub ? sub.split('@')[0] : 'Profile');

    const logout = useLogout()
    const { currency, setCurrency } = useCurrency();
    const [isCurrencyDropdownOpen, setIsCurrencyDropdownOpen] = useState(false);
    const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState(false);
    const location = useLocation();
    const isHomePage = location.pathname === '/';
    const [scrolled, setScrolled] = useState(false);
    const profileDropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (profileDropdownRef.current && !profileDropdownRef.current.contains(event.target as Node)) {
                setIsProfileDropdownOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 50);
        };
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    const navLinks = [
        { name: 'Catalog', path: '/catalog' },
        { name: 'About', path: '/about' },
    ];

    return (
        <nav className={`${isHomePage ? 'absolute' : 'sticky'} top-0 w-full z-50 transition-all duration-300 ${scrolled || !isHomePage ? 'bg-gradient-to-r from-[rgb(var(--color-burgundy))]/90 to-[rgb(var(--color-burgundy))]/40 backdrop-blur-md py-3 shadow-md' : 'bg-gradient-to-r from-[rgb(var(--color-burgundy))]/90 to-[rgb(var(--color-burgundy))]/40 py-5'}`}>
            <div className="max-w-7xl mx-auto px-4 md:px-8 lg:px-16 flex justify-between items-center relative">
                
                <Link to="/" className="flex items-center gap-3 group">
                    <img src="/KWATERA_logo.png" alt="Logo" className="h-10 md:h-12 object-contain drop-shadow-md brightness-0 invert" />
                    <span className="font-normal text-2xl tracking-widest text-white drop-shadow-md hidden sm:block">KWATERA</span>
                </Link>


                <div className="hidden lg:flex items-center gap-8 absolute left-1/2 -translate-x-1/2">
                    {navLinks.map((link) => (
                        <Link 
                            key={link.name} 
                            to={link.path}
                            className="text-white hover:text-white/80 font-medium text-sm tracking-widest uppercase transition-colors"
                        >
                            {link.name}
                        </Link>
                    ))}
                </div>


                <div className="flex items-center gap-4">

                    <div className="relative mr-4 pr-4">
                        <button
                            onClick={() => setIsCurrencyDropdownOpen(!isCurrencyDropdownOpen)}
                            className="text-white hover:text-white/80 font-medium text-sm transition-colors uppercase"
                        >
                            {currency}
                        </button>
                        {isCurrencyDropdownOpen && (
                            <div className="absolute top-full right-0 mt-2 w-24 bg-card rounded-xl shadow-lg z-50 overflow-hidden py-1 border border-gray-100">
                                {['PLN', 'EUR', 'USD'].map((curr) => (
                                    <button
                                        key={curr}
                                        onClick={() => {
                                            setCurrency(curr);
                                            setIsCurrencyDropdownOpen(false);
                                        }}
                                        className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 transition-colors ${currency === curr ? 'font-bold text-[rgb(var(--color-burgundy))] bg-gray-50' : 'text-title'}`}
                                    >
                                        {curr}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    {isLoggedIn ? (
                        <div className="relative ml-2" ref={profileDropdownRef}>
                            <button 
                                onClick={() => setIsProfileDropdownOpen(!isProfileDropdownOpen)}
                                className="flex items-center gap-2 rounded-full bg-white/10 border-2 border-white/40 pl-1.5 pr-4 py-1.5 text-white hover:bg-white hover:text-[rgb(var(--color-burgundy))] transition-all shadow-md"
                            >
                                <div className="w-7 h-7 rounded-full bg-white/20 flex items-center justify-center">
                                    <User size={16} />
                                </div>
                                <span className="font-medium text-sm hidden md:block max-w-[100px] truncate">{displayName}</span>
                            </button>

                            {isProfileDropdownOpen && (
                                <div className="absolute top-full right-0 mt-2 w-56 bg-card rounded-2xl shadow-xl z-50 overflow-hidden border border-gray-100 py-2">
                                    <div className="px-4 py-3 border-b border-gray-100 mb-1">
                                        <p className="text-sm font-bold text-title">My Account</p>
                                    </div>
                                    
                                    <Link to="/profile" className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors">
                                        <Settings size={16} />
                                        My Profile
                                    </Link>
                                    
                                    {(!userRoles.includes("ROLE_ADMIN") && !userRoles.includes("ROLE_OWNER")) && (
                                        <Link to="/my-reservations" className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors">
                                            <Calendar size={16} />
                                            My Reservations
                                        </Link>
                                    )}

                                    {(userRoles.includes("ROLE_ADMIN") || userRoles.includes("ROLE_OWNER")) && (
                                        <>
                                            <Link to="/admin/reservations" className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors">
                                                <Calendar size={16} />
                                                Reservations
                                            </Link>
                                            <Link to="/admin/dashboard" className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors">
                                                <LayoutDashboard size={16} />
                                                Dashboard
                                            </Link>
                                        </>
                                    )}
                                    
                                    <div className="h-px bg-gray-100 my-1"></div>
                                    
                                    <button 
                                        onClick={logout}
                                        className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 transition-colors font-medium"
                                    >
                                        <LogOut size={16} />
                                        Logout
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="flex items-center gap-6 ml-2">
                            <Link 
                                to="/login"
                                className="text-white hover:text-white/80 font-medium text-sm transition-colors uppercase tracking-wide"
                            >
                                Log in
                            </Link>
                            <Link 
                                to="/register"
                                className="text-white hover:text-white/80 font-medium text-sm transition-colors uppercase tracking-wide"
                            >
                                Sign up
                            </Link>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
}