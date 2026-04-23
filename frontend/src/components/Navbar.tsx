import '../App.css'
import {Link} from "react-router-dom"
import {useLogout} from "./Logout.tsx"

export default function Navbar() {
    const isLoggedIn = localStorage.getItem("token") !== null;
    const userRole = localStorage.getItem("userRole");
    const logout = useLogout();

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

                {isLoggedIn && (userRole === 'ADMIN' || userRole === 'OWNER') && (
                    <Link
                        to="/admin/reservations"
                        className="hover:text-[rgb(var(--color-burgundy))] transition"
                    >
                        Reservations
                    </Link>
                )}

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
            </div>
        </nav>
    )
}