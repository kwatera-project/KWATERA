import React from "react";
import { Navigate } from "react-router-dom";
import { getUserRoles } from "../utils/jwtUtils";

interface ProtectedRouteProps {
    children: React.ReactNode;
    allowedRoles?: string[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
    const token = localStorage.getItem("token");
    const isAuthenticated = !!token;
    
    if (!isAuthenticated) return <Navigate to="/login" replace />;

    if (!allowedRoles || allowedRoles.length === 0) return <>{children}</>;

    const userRoles = getUserRoles(token);
    
    const hasAllowedRole = userRoles.some(role => allowedRoles.includes(role));

    if (!hasAllowedRole) return <Navigate to="/" replace />;

    return <>{children}</>;
}

export default ProtectedRoute;