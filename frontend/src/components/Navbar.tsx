import '../App.css'
import {Link} from "react-router-dom"
import {useLogout} from "./Logout.tsx"
import {getUserRoles} from "../utils/jwtUtils.ts"

export default function Navbar() {

    const token = localStorage.getItem("token");
    const userRoles = getUserRoles(token);
    const isLoggedIn = !!token;

    const logout = useLogout()

    return (
        <nav className="flex justify-between p-4 bg-card shadow-md">

            <div className="font-bold text-title">
                KWATERA
            </div>

            <div className="flex gap-6">
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
            </div>

        </nav>
    )
}