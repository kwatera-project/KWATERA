import '../App.css'
import { Link } from "react-router-dom"
import {useLogout} from "./Logout.tsx"

export default function Navbar() {
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
                <Link
                    to="/register"
                    className="hover:text-[rgb(var(--color-burgundy))] transition"
                >
                    Register
                </Link>
                <Link
                    to="/login"
                    className="hover:text-[rgb(var(--color-burgundy))] transition"
                >
                    Login
                </Link>
                <button onClick={useLogout}>
                    Logout
                </button>
            </div>

        </nav>
    )
}