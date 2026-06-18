import { IS_DEMO_MODE } from "../api/apiConfig";

export default function DemoModeBanner() {
    if (!IS_DEMO_MODE) return null;

    return (
        <div className="fixed bottom-3 right-3 z-[900] max-w-[calc(100vw-1.5rem)] rounded-full border border-amber-200 bg-amber-50/95 px-3 py-1.5 text-[11px] sm:text-xs font-bold text-amber-900 shadow-sm backdrop-blur">
            Demo mode: sample data only, no server writes.
        </div>
    );
}
