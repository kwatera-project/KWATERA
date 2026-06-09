import { IS_DEMO_MODE } from "../api/apiConfig";

export default function DemoModeBanner() {
    if (!IS_DEMO_MODE) return null;

    return (
        <div className="sticky top-0 z-[1100] bg-amber-50 border-b border-amber-200 text-amber-900 text-center text-xs sm:text-sm font-bold px-4 py-2">
            Demo mode: dane przykładowe, bez zapisu na serwerze
        </div>
    );
}
