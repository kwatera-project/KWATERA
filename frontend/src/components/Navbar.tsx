import '../App.css'
import {Link} from "react-router-dom"
import {useLogout} from "./Logout.tsx"
import {getUserRoles} from "../utils/jwtUtils.ts"
import {useCurrency} from "../contexts/CurrencyContext"

export default function Navbar() {

    const token = localStorage.getItem("token");
    const userRoles = getUserRoles(token);
    const isLoggedIn = !!token;

    const logout = useLogout()
    const { currency, setCurrency } = useCurrency();

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
                    <Link
                        to="/admin/reservations"
                        className="hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        Reservations
                    </Link>
                )}

                <div className="flex items-center ml-4">
                    <select
                        value={currency}
                        onChange={(e) => setCurrency(e.target.value)}
                        className="bg-card text-title font-medium border border-[#DACDCA] rounded-xl px-3 py-1 cursor-pointer hover:border-[rgb(var(--color-burgundy))] focus:outline-none focus:border-[rgb(var(--color-burgundy))] transition appearance-none pr-8"
                        style={{
                            backgroundImage: `url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e")`,
                            backgroundRepeat: 'no-repeat',
                            backgroundPosition: 'calc(100% - 8px) center',
                            backgroundSize: '1em'
                        }}
                    >
                        <option value="PLN" className="text-gray-800">PLN</option>
                        <option value="EUR" className="text-gray-800">EUR</option>
                        <option value="USD" className="text-gray-800">USD</option>
                    </select>
                </div>
            </div>

        </nav>
    )
}