import { format } from "date-fns";

export function formatSearchDate(date: Date) {
    return format(date, "yyyy-MM-dd");
}
