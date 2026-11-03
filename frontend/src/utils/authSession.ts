export function subscribeAuthChanges(onChange: () => void) {
    function onStorage(event: StorageEvent) {
        if (event.storageArea === localStorage && (event.key === "token" || event.key === null)) onChange();
    }
    window.addEventListener("storage", onStorage);
    window.addEventListener("kwatera-auth", onChange);
    window.addEventListener("kwatera-demo-auth", onChange);
    return () => {
        window.removeEventListener("storage", onStorage);
        window.removeEventListener("kwatera-auth", onChange);
        window.removeEventListener("kwatera-demo-auth", onChange);
    };
}
