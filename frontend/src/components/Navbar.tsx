import '../App.css'
import {Link, useLocation} from "react-router-dom"
import {useLogout} from "./Logout.tsx"
import {getUserRoles, decodeJwt} from "../utils/jwtUtils.ts"
import {useState, useEffect, useRef} from "react"
import {useCurrency} from "../contexts/CurrencyContext"
import {User, LogOut, LayoutDashboard, Calendar, Settings, Home, FileClock, Menu, X} from 'lucide-react'
import {IS_DEMO_MODE} from "../api/apiConfig.ts"

interface NavbarProps {
    isSubpage?: boolean;
}

export default function Navbar({isSubpage}: NavbarProps = {}) {
    const [, setAuthVersion] = useState(0);
    const token = localStorage.getItem("token");
    const userRoles = getUserRoles(token);
    const isLoggedIn = !!token;
    const payload = token ? decodeJwt(token) : null;
    const sub = payload?.sub as string | undefined;
    const firstName = payload?.firstName as string | undefined;
    const displayName = firstName || (sub ? sub.split('@')[0] : 'Profile');

    const logout = useLogout()
    const {currency, setCurrency} = useCurrency();
    const [isCurrencyDropdownOpen, setIsCurrencyDropdownOpen] = useState(false);
    const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const location = useLocation();
    const isHomePage = location.pathname === '/';
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMobile, setIsMobile] = useState(false);
    const profileDropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleResize = () => {
            setIsMobile(window.innerWidth < 1024);
        };
        handleResize();
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 50);
        };
        handleScroll();
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    useEffect(() => {
        const handleDemoAuth = () => setAuthVersion((value) => value + 1);
        window.addEventListener("kwatera-demo-auth", handleDemoAuth);
        window.addEventListener("storage", handleDemoAuth);
        return () => {
            window.removeEventListener("kwatera-demo-auth", handleDemoAuth);
            window.removeEventListener("storage", handleDemoAuth);
        };
    }, []);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (profileDropdownRef.current && !profileDropdownRef.current.contains(event.target as Node)) {
                setIsProfileDropdownOpen(false);
            }
        }

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);


    // Auto-close all dropdowns when the route changes
    useEffect(() => {
        const timer = setTimeout(() => {
            setIsProfileDropdownOpen(false);
            setIsCurrencyDropdownOpen(false);
            setIsMobileMenuOpen(false);
        }, 0);
        return () => clearTimeout(timer);
    }, [location.pathname]);

    const navLinks = [
        {name: 'Catalog', path: '/properties'},
        {name: 'About', path: '/about'},
    ];

    const isEffectiveSubpage = isSubpage !== undefined ? isSubpage : !isHomePage;
    const isFullScreenPage = ['/login', '/register', '/payment-cancel'].some(path => location.pathname.includes(path));
    const positionClass = (isEffectiveSubpage && !isFullScreenPage) ? 'sticky top-0' : 'fixed top-0';
    
    const isNavbarWhite = isScrolled || isEffectiveSubpage || isFullScreenPage || isMobileMenuOpen;

    const bgAndPaddingClass = isNavbarWhite
        ? (isMobile ? 'bg-solid-white border-b border-gray-200 py-4 shadow-sm' : 'bg-white/95 backdrop-blur-md border-b border-gray-200 py-4 shadow-sm')
        : 'bg-transparent py-5';

    const containerClass = "max-w-7xl mx-auto px-4 md:px-8 lg:px-16 flex justify-between items-center relative";

    return (
        <nav className={`${positionClass} w-full z-[20000] transition-all duration-300 ease-in-out ${bgAndPaddingClass}`}>
            <div className={containerClass}>

                <Link to="/" className="flex items-center gap-2.5 group leading-none">
                    <svg
                        viewBox="0 -79 2000 2000"
                        width="40"
                        height="40"
                        xmlns="http://www.w3.org/2000/svg"
                        className={`w-10 h-10 shrink-0 self-center transition-all duration-300 ${isNavbarWhite ? 'text-stone-800' : 'text-white drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}
                    >
                        <path fill="currentColor"
                              d="M 1001.8145 497.94141 C 989.27928 497.36938 976.614 502.27509 967.51953 512.4668 L 592.09375 933.18555 C 591.50163 933.8491 590.93365 934.52645 590.38867 935.21484 C 581.89305 942.6862 576.54688 953.64565 576.54688 965.90039 L 576.54688 1303.418 C 576.54688 1326.0369 594.75603 1344.2461 617.375 1344.2461 L 619.84961 1344.2461 C 642.46858 1344.2461 660.67969 1326.0369 660.67969 1303.418 L 660.67969 995.45898 L 1000.7637 614.3457 L 1079.8535 705.35352 C 1078.5346 706.41641 1077.2728 707.58187 1076.0801 708.84766 L 793.69727 1008.5312 C 788.97314 1013.5448 785.94625 1019.4865 784.5918 1025.6855 C 783.43366 1029.0742 782.80664 1032.7131 782.80664 1036.5039 L 782.80664 1311.3652 C 782.80664 1329.7854 797.63455 1344.6152 816.05469 1344.6152 L 833.6875 1344.6152 C 834.53571 1344.6152 835.3771 1344.5833 836.20898 1344.5215 L 836.20898 1344.8203 L 1373.168 1344.8203 C 1374.2347 1344.9076 1375.3144 1344.9531 1376.4043 1344.9531 L 1382.9395 1344.9531 C 1404.4337 1344.9531 1421.7383 1327.6486 1421.7383 1306.1543 L 1421.7383 985.41992 C 1421.7383 983.86824 1421.6481 982.33901 1421.4727 980.83594 C 1422.7897 968.69313 1419.2703 956.06425 1410.748 946.25781 L 1043.2305 523.35938 C 1041.5437 521.4184 1039.7304 519.66869 1037.8164 518.11133 C 1036.3094 516.18661 1034.6231 514.36015 1032.7578 512.6582 L 1029.082 509.30469 C 1021.2339 502.1439 1011.564 498.38631 1001.8145 497.94141 z M 1138.8477 773.23633 L 1337.6055 1001.9453 L 1337.6055 1259.998 L 866.9375 1259.998 L 866.9375 1061.8027 L 1138.8477 773.23633 z"/>
                    </svg>
                    <span className={`font-medium text-2xl tracking-widest hidden sm:block self-center transition-all duration-300 ${isNavbarWhite ? 'text-stone-800' : 'text-white drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}>KWATERA</span>
                </Link>

                <div className="hidden lg:flex items-center gap-10 absolute left-1/2 -translate-x-1/2">
                    {navLinks.map((link) => (
                        <Link
                            key={link.name}
                            to={link.path}
                            className={`font-bold text-[15px] tracking-[0.15em] uppercase transition-all duration-300 ${isNavbarWhite ? 'text-stone-800 hover:text-brand-primary' : 'text-white hover:text-white/80 drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}
                        >
                            {link.name}
                        </Link>
                    ))}
                </div>

                <div className="flex items-center gap-6 md:gap-8">
                    <div className="relative hidden lg:block">
                        <button
                            onClick={() => setIsCurrencyDropdownOpen(!isCurrencyDropdownOpen)}
                            className={`font-medium text-sm transition-all uppercase tracking-widest flex items-center gap-1.5 focus:outline-none ${isNavbarWhite ? 'text-stone-800 hover:text-[#8B4513]' : 'text-white hover:text-white/80 drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}
                        >
                            {currency}
                        </button>

                        {isCurrencyDropdownOpen && (
                            <div className="absolute top-full right-0 mt-3 w-28 bg-solid-white rounded-2xl shadow-xl z-[21000] overflow-hidden py-1.5 border border-[#DACDCA]/40">
                                {['PLN', 'EUR', 'USD'].map((curr) => (
                                    <button
                                        key={curr}
                                        onClick={() => {
                                            setCurrency(curr);
                                            setIsCurrencyDropdownOpen(false);
                                        }}
                                        className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 transition-colors ${currency === curr ? 'font-black text-[rgb(var(--color-burgundy))] bg-gray-50' : 'text-title font-medium'}`}
                                    >
                                        {curr}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    <div
                        className={`h-5 w-px hidden lg:block transition-all duration-300 ${isNavbarWhite ? 'bg-stone-300' : 'bg-white/20'}`}></div>

                    {isLoggedIn ? (
                        <div className="relative" ref={profileDropdownRef}>
                            <button
                                onClick={() => setIsProfileDropdownOpen(!isProfileDropdownOpen)}
                                className={`flex items-center gap-2 rounded-full transition-all duration-300 focus:outline-none pl-1.5 pr-4 py-1.5 border ${isNavbarWhite ? 'bg-transparent border-stone-300 text-stone-800 hover:bg-stone-100/50' : 'bg-[rgb(var(--color-burgundy))] border-transparent text-white hover:bg-[rgb(var(--color-burgundy-hover))] shadow-md active:scale-95'}`}
                            >
                            <div className={`w-7 h-7 rounded-full flex items-center justify-center transition-all duration-300 ${isNavbarWhite ? 'bg-stone-100 text-stone-800 border border-stone-200' : 'bg-white/20 text-white'}`}>
                                <User size={16} />
                            </div>
                            <span className={`font-semibold text-xs tracking-wider uppercase hidden md:block max-w-[100px] truncate transition-all duration-300 ${isNavbarWhite ? '' : 'text-white drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}>
                              {displayName}
                            </span>
                            </button>

                            {isProfileDropdownOpen && (
                                <div
                                    className="absolute top-full right-0 mt-3 w-56 bg-solid-white rounded-2xl shadow-2xl z-[21000] overflow-hidden border border-[#DACDCA]/40 py-2 animate-in fade-in slide-in-from-top-2 duration-200">
                                    <div className="px-4 py-3 border-b border-gray-100 mb-1">
                                        <p className="text-xs font-bold uppercase tracking-wider text-details">My
                                            Account</p>
                                    </div>

                                    <Link to="/profile"
                                          className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors font-semibold">
                                        <Settings size={16}/>
                                        My Profile
                                    </Link>

                                    {(!userRoles.includes("ROLE_ADMIN") && !userRoles.includes("ROLE_OWNER")) && (
                                        <Link to="/my-reservations"
                                              className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors font-semibold">
                                            <Calendar size={16}/>
                                            My Reservations
                                        </Link>
                                    )}

                                    {(userRoles.includes("ROLE_ADMIN") || userRoles.includes("ROLE_OWNER")) && (
                                        <>
                                        <Link to="/admin/reservations"
                                              className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors font-semibold">
                                            <Calendar size={16}/>
                                            Reservations
                                        </Link>
                                        <Link to="/admin/dashboard"
                                              className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors font-semibold">
                                            <LayoutDashboard size={16}/>
                                            Dashboard
                                        </Link>
                                        {userRoles.includes("ROLE_ADMIN") && (
                                            <Link to="/admin/logs"
                                                  className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors font-semibold">
                                                <FileClock size={16}/>
                                                System Logs
                                            </Link>
                                        )}
                                        {userRoles.includes("ROLE_OWNER") && (
                                            <Link to="/owner/properties"
                                                className="flex items-center gap-3 px-4 py-2.5 text-sm text-[rgb(var(--color-burgundy))] hover:bg-gray-50 transition-colors font-semibold">
                                                <Home size={16} />
                                                Manage properties
                                            </Link>
                                        )}
                                        </>
                                        )}

                                    <div className="h-px bg-gray-100 my-1"></div>

                                    <button
                                        onClick={logout}
                                        className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 transition-colors font-bold"
                                    >
                                        <LogOut size={16}/>
                                        Logout
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="flex items-center gap-6">
                            {IS_DEMO_MODE ? (
                                <Link
                                    to="/login"
                                    className={`font-bold text-sm tracking-wide uppercase transition-all duration-300 ${isNavbarWhite ? 'text-stone-800 hover:bg-stone-100 px-4 py-2 rounded-full' : 'text-white hover:text-gray-200 drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}
                                >
                                    DEMO LOGIN
                                </Link>
                            ) : (
                                <>
                                <Link
                                to="/login"
                                className={`font-bold text-sm tracking-wide uppercase transition-all duration-300 ${isNavbarWhite ? 'text-stone-800 hover:bg-stone-100 px-4 py-2 rounded-full' : 'text-white hover:text-gray-200 drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}
                            >
                                LOG IN
                            </Link>
                            <Link
                                to="/register"
                                className="bg-[rgb(var(--color-burgundy))] hover:bg-[rgb(var(--color-burgundy-hover))] text-white font-bold text-sm tracking-wide px-5 py-2 rounded-full transition-all duration-300 shadow-md active:scale-95"
                            >
                                SIGN UP
                            </Link>
                                </>
                            )}
                        </div>
                    )}

                    <button
                        onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                        className={`lg:hidden p-2 rounded-lg transition-colors focus:outline-none cursor-pointer ${isNavbarWhite ? 'text-stone-800 hover:bg-stone-100' : 'text-white hover:bg-white/10 drop-shadow-[0_2px_2px_rgba(0,0,0,0.8)]'}`}
                    >
                        {isMobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
                    </button>
                </div>
            </div>

            {isMobileMenuOpen && (
                <div className="lg:hidden absolute top-full left-0 w-full bg-solid-white border-b border-gray-200 shadow-xl py-4 px-6 flex flex-col gap-4 animate-in fade-in slide-in-from-top-2 duration-200 z-[19000]">
                    {navLinks.map((link) => (
                        <Link
                            key={link.name}
                            to={link.path}
                            onClick={() => setIsMobileMenuOpen(false)}
                            className="font-bold text-base tracking-[0.1em] uppercase text-stone-800 hover:text-[#42211D] py-2.5 border-b border-gray-100 last:border-b-0"
                        >
                            {link.name}
                        </Link>
                    ))}

                    <div className="py-2.5 flex flex-col gap-2">
                        <span className="text-xs font-bold uppercase tracking-wider text-stone-400">Currency</span>
                        <div className="flex gap-2">
                            {['PLN', 'EUR', 'USD'].map((curr) => (
                                <button
                                    key={curr}
                                    onClick={() => {
                                        setCurrency(curr);
                                        setIsMobileMenuOpen(false);
                                    }}
                                    className={`px-4 py-2 rounded-xl text-sm font-bold transition-all ${
                                        currency === curr
                                            ? 'bg-[rgb(var(--color-burgundy))] text-white shadow-sm'
                                            : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                                    }`}
                                >
                                    {curr}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </nav>
    );
}
