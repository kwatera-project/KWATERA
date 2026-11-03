import { useSyncExternalStore } from "react";

function subscribe(onChange: () => void) {
    function onStorage(event: StorageEvent) {
        if (event.storageArea === localStorage && (event.key === "token" || event.key === null)) onChange();
    }
    window.addEventListener("storage", onStorage);
    window.addEventListener("kwatera-demo-auth", onChange);
    return () => {
        window.removeEventListener("storage", onStorage);
        window.removeEventListener("kwatera-demo-auth", onChange);
    };
}

// Match the navbar's existing cross-tab and same-tab demo authentication events.
export function useAuthToken() {
    return useSyncExternalStore(subscribe, () => localStorage.getItem("token"));
}
