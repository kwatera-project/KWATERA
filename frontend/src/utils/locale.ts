import { enGB, pl } from "date-fns/locale";

export function getLocaleCode(language: string) {
    return language.startsWith("pl") ? "pl-PL" : "en-GB";
}

export function getDateFnsLocale(language: string) {
    return language.startsWith("pl") ? pl : enGB;
}
