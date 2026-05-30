export interface JwtPayload {
    userId?: string;
    role?: string | string[];
    firstName?: string;
    lastName?: string;
    [key: string]: unknown;
}

export function decodeJwt(token: string | null): JwtPayload | null {
    if (!token) return null;
    
    try {
        const payloadBase64Url = token.split(".")[1];
        if (!payloadBase64Url) return null;

        const base64 = payloadBase64Url.replace(/-/g, "+").replace(/_/g, "/");
        
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split("")
                .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
                .join("")
        );
        
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error("Failed to parse JWT token", e);
        return null;
    }
}

export function getUserRoles(token: string | null): string[] {
    const payload = decodeJwt(token);
    if (!payload || !payload.role) return [];

    if (Array.isArray(payload.role)) {
        return payload.role;
    }
    
    if (typeof payload.role === 'string') {
        return [payload.role];
    }

    return [];
}
