/**
 * Shared API configuration for the KWATERA frontend.
 * All API calls must go through the API Gateway.
 */
export const IS_DEMO_MODE = import.meta.env.VITE_DEMO_MODE === "true";
export const APP_BASE_PATH = import.meta.env.VITE_BASE_PATH || "/";
export const GATEWAY_BASE_URL = IS_DEMO_MODE
    ? ""
    : (import.meta.env.VITE_API_BASE_URL || "http://localhost:8090");
