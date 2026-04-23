import { Navigate } from "react-router-dom";
import type {JSX} from "react";

export default function ProtectedRoute({ children, allowedRoles }: { children: JSX.Element, allowedRoles: string[] }) {
    const userRole = localStorage.getItem("userRole");
    const isAuthenticated = !!localStorage.getItem("token");

    if (!isAuthenticated) return <Navigate to="/login" replace />;
    if (userRole && !allowedRoles.includes(userRole)) return <Navigate to="/" replace />;

    return children;
}