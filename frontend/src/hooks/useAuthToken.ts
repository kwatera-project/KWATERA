import { useSyncExternalStore } from "react";
import { subscribeAuthChanges } from "../utils/authSession";

// Observe cross-tab storage and same-tab login, logout and demo authentication changes.
export function useAuthToken() {
    return useSyncExternalStore(subscribeAuthChanges, () => localStorage.getItem("token"));
}
