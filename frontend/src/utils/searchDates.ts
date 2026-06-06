import { format, isValid, parseISO } from "date-fns";

export function formatSearchDate(date: Date) {
    return format(date, "yyyy-MM-dd");
}

export function parseSearchDate(value: string | null) {
    if (!value) return null;
    const parsed = parseISO(value);
    return isValid(parsed) ? parsed : null;
}

export function parseGuests(value: string | null) {
    if (!value) return null;
    const guests = Number(value);
    return Number.isInteger(guests) && guests >= 1 ? guests : null;
}
