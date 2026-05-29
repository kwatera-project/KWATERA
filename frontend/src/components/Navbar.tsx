import '../App.css'
import {Link} from "react-router-dom"
import {useLogout} from "./Logout.tsx"
import {getUserRoles} from "../utils/jwtUtils.ts"
import {useState} from "react"
import {useCurrency} from "../contexts/CurrencyContext"

export default function Navbar() {

    const token = localStorage.getItem("token");
    const userRoles = getUserRoles(token);
    const isLoggedIn = !!token;

    const logout = useLogout()
    const { currency, setCurrency } = useCurrency();
    const [isCurrencyDropdownOpen, setIsCurrencyDropdownOpen] = useState(false);

    return (
        <nav className="flex justify-between items-center p-4 bg-card shadow-md">

            <div className="font-bold text-title">
                KWATERA
            </div>

            <div className="flex items-center gap-6">
                <Link
                    to="/"
                    className="hover:text-[rgb(var(--color-burgundy))] transition"
                >
                    Home
                </Link>
                {isLoggedIn ? (
                    <button
                        onClick={logout}
                        className="hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        Logout
                    </button>
                ) : (
                    <Link
                        to="/login"
                        className="hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        Login
                    </Link>
                )}
                <Link
                    to="/catalog"
                    className="hover:text-[rgb(var(--color-burgundy))] transition"
                >
                    Catalog
                </Link>

                {isLoggedIn && (
                    <Link
                        to="/profile"
                        className="hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        Profile
                    </Link>
                )}

                {isLoggedIn && userRoles.includes("ROLE_GUEST") && (
                    <Link
                        to="/my-reservations"
                        className="hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        My reservations
                    </Link>
                )}

                {isLoggedIn && (userRoles.includes("ROLE_ADMIN") || userRoles.includes("ROLE_OWNER"))  && (
                    <>
                        <Link
                            to="/admin/dashboard"
                            className="hover:text-[rgb(var(--color-burgundy))] transition"
                        >
                            Dashboard
                        </Link>
                        <Link
                            to="/admin/reservations"
                            className="hover:text-[rgb(var(--color-burgundy))] transition"
                        >
                            Reservations
                        </Link>
                    </>
                )}

                <div className="relative ml-4">
                    <button
                        onClick={() => setIsCurrencyDropdownOpen(!isCurrencyDropdownOpen)}
                        className="bg-transparent border-none focus:outline-none cursor-pointer hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        {currency}
                    </button>
                    {isCurrencyDropdownOpen && (
                        <div className="absolute right-0 mt-2 w-24 bg-card border border-[#DACDCA] rounded-xl shadow-lg z-50 overflow-hidden">
                            <ul className="flex flex-col text-sm">
                                {['PLN', 'EUR', 'USD'].map((curr) => (
                                    <li key={curr}>
                                        <button
                                            onClick={() => {
                                                setCurrency(curr);
                                                setIsCurrencyDropdownOpen(false);
                                            }}
                                            className={`w-full text-left px-4 py-2 hover:bg-[#F7F7F7] hover:text-[rgb(var(--color-burgundy))] transition ${currency === curr ? 'font-bold text-[rgb(var(--color-burgundy))] bg-[#F7F7F7]' : 'text-[#1A1A1A]'}`}
                                        >
                                            {curr}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            </div>

        </nav>
    )
}