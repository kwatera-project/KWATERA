import type { UnitSettlementItem } from "../types/property";

type CurrencyInfo = {
    displayCurrency: string;
    exchangeRate: number;
};

export type WaterUsageRange = {
    minUsageM3: number;
    maxUsageM3: number;
};

export const M3_LABEL = "m\u00B3";

export function calculateStayNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn).getTime();
    const end = new Date(checkOut).getTime();

    if (Number.isNaN(start) || Number.isNaN(end)) {
        return 1;
    }

    return Math.max(1, Math.ceil((end - start) / (1000 * 60 * 60 * 24)));
}

export function calculateWaterUsageRange(capacity: number, nights: number): WaterUsageRange {
    const expectedUsageM3 = capacity * Math.max(1, nights) * 0.1;

    return {
        minUsageM3: expectedUsageM3 * 0.2,
        maxUsageM3: expectedUsageM3 * 3.0,
    };
}

export function findWaterUsageTariff(items: UnitSettlementItem[]): UnitSettlementItem | undefined {
    return items.find((item) =>
        item.settlementItemType === "WATER" &&
        item.billingType === "PER_USAGE" &&
        item.measurementUnit === "M3" &&
        typeof item.pricePerUnit === "number"
    );
}

export function formatUsageRange(range: WaterUsageRange): string {
    return `${formatNumber(range.minUsageM3)}-${formatNumber(range.maxUsageM3)} ${M3_LABEL}`;
}

export function formatMoneyRange(
    range: WaterUsageRange,
    pricePerUnitPln: number,
    currencyInfo?: CurrencyInfo
): string {
    const currency = currencyInfo?.displayCurrency && currencyInfo.displayCurrency !== "PLN" && currencyInfo.exchangeRate > 0
        ? currencyInfo.displayCurrency
        : "PLN";
    const divisor = currency === "PLN" ? 1 : currencyInfo!.exchangeRate;
    const minCost = (range.minUsageM3 * pricePerUnitPln) / divisor;
    const maxCost = (range.maxUsageM3 * pricePerUnitPln) / divisor;

    return `${formatNumber(minCost)}-${formatNumber(maxCost)} ${currency}`;
}

export function formatWaterRate(pricePerUnitPln: number, currencyInfo?: CurrencyInfo): string {
    const plnRate = `${formatNumber(pricePerUnitPln, 2, 2)} PLN/${M3_LABEL}`;

    if (!currencyInfo || currencyInfo.displayCurrency === "PLN" || currencyInfo.exchangeRate <= 0) {
        return plnRate;
    }

    const convertedRate = pricePerUnitPln / currencyInfo.exchangeRate;
    return `${plnRate} (~${formatNumber(convertedRate, 2, 2)} ${currencyInfo.displayCurrency}/${M3_LABEL})`;
}

export function getRatePerLiterTooltip(pricePerUnitPln: number, currencyInfo?: CurrencyInfo): string {
    if (!currencyInfo || currencyInfo.displayCurrency === "PLN" || currencyInfo.exchangeRate <= 0) {
        return `1 ${M3_LABEL} = 1000 L, so ${formatNumber(pricePerUnitPln, 2, 2)} PLN/${M3_LABEL} = ${formatNumber(pricePerUnitPln / 1000, 4, 4)} PLN/L.`;
    }

    const convertedRate = pricePerUnitPln / currencyInfo.exchangeRate;
    return `1 ${M3_LABEL} = 1000 L. ${formatNumber(pricePerUnitPln, 2, 2)} PLN/${M3_LABEL} \u2248 ${formatNumber(convertedRate, 2, 2)} ${currencyInfo.displayCurrency}/${M3_LABEL}, so \u2248 ${formatNumber(convertedRate / 1000, 4, 4)} ${currencyInfo.displayCurrency}/L.`;
}

function formatNumber(value: number, maximumFractionDigits = 2, minimumFractionDigits = 0): string {
    return new Intl.NumberFormat("en-US", {
        minimumFractionDigits,
        maximumFractionDigits,
    }).format(value);
}
