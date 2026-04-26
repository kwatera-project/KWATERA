import { Navigate } from "react-router-dom";
import type {JSX} from "react";
import { getUserRoles } from "../utils/jwtUtils";

export default function ProtectedRoute({ children, allowedRoles }: { children: JSX.Element, allowedRoles: string[] }) {
    const token = localStorage.getItem("token");
    const isAuthenticated = !!token;
    
    if (!isAuthenticated) return <Navigate to="/login" replace />;

    const userRoles = getUserRoles(token);
    
    const hasAllowedRole = userRoles.some(role => allowedRoles.includes(role));

    if (!hasAllowedRole) return <Navigate to="/" replace />;

    return children;
}