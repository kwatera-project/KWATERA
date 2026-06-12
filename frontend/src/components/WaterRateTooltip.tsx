type WaterRateTooltipProps = {
    text: string;
    align?: "left" | "right";
    iconClassName?: string;
    panelClassName?: string;
};

export default function WaterRateTooltip({
    text,
    align = "left",
    iconClassName = "border-brand-accent text-brand-muted bg-white",
    panelClassName = "border-brand-accent bg-white text-brand-main shadow-xl",
}: WaterRateTooltipProps) {
    const alignmentClass = align === "right" ? "right-0" : "left-0";

    return (
        <span className="group relative isolate inline-flex overflow-visible">
            <button
                type="button"
                aria-label="Water rate conversion"
                className={`inline-flex h-4 w-4 items-center justify-center rounded-full border text-[10px] font-bold cursor-help focus:outline-none focus:ring-2 focus:ring-brand-primary/20 ${iconClassName}`}
            >
                ?
            </button>
            <span
                role="tooltip"
                className={`pointer-events-none absolute top-full ${alignmentClass} z-[9999] mt-2 hidden w-64 max-w-[min(16rem,calc(100vw-2rem))] rounded-lg border px-3 py-2 text-left text-xs font-medium leading-snug group-hover:block group-focus-within:block ${panelClassName}`}
            >
                {text}
            </span>
        </span>
    );
}
