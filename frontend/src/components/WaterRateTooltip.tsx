type WaterRateTooltipProps = {
    text: string;
    iconClassName?: string;
    panelClassName?: string;
};

export default function WaterRateTooltip({
    text,
    iconClassName = "border-brand-accent text-brand-muted bg-white",
    panelClassName = "bg-white border-brand-accent text-brand-main shadow-lg",
}: WaterRateTooltipProps) {
    return (
        <span className="relative inline-flex group">
            <button
                type="button"
                aria-label="Water rate conversion"
                className={`inline-flex h-4 w-4 items-center justify-center rounded-full border text-[10px] font-bold cursor-help focus:outline-none focus:ring-2 focus:ring-brand-primary/20 ${iconClassName}`}
            >
                ?
            </button>
            <span
                role="tooltip"
                className={`pointer-events-none absolute bottom-full left-1/2 z-30 mb-2 hidden w-56 -translate-x-1/2 rounded-lg px-3 py-2 text-left text-xs font-medium leading-snug group-hover:block group-focus-within:block ${panelClassName}`}
            >
                {text}
            </span>
        </span>
    );
}
