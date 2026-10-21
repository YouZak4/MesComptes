import type { TypeOperation } from "@/types/comptes.ts";

const euros = new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
});

const dateCourte = new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "short",
});

const dateLongue = new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "long",
    year: "numeric",
});

const pourcentage = new Intl.NumberFormat("fr-FR", {
    style: "percent",
    maximumFractionDigits: 0,
});

// 0.48 → « 48 % »
export function formatPourcentage(part: number): string {
    return pourcentage.format(part);
}

// Négatif : le vrai signe moins (U+2212), et non le tiret que donne Intl
export function formatMontant(montant: number): string {
    return euros.format(montant).replace("-", "−");
}

// Montant d'une opération : toujours positif côté back, le type donne le signe
export function formatMontantSigne(
    montant: number,
    type: TypeOperation
): string {
    return (type === "DEPENSE" ? "−" : "+") + euros.format(montant);
}

// Écart avec la période précédente, toujours signé ; « Identique » s'il est nul
export function formatEcart(ecart: number): string {
    if (ecart === 0) return "Identique à la période précédente";
    const montant = (ecart > 0 ? "+" : "−") + euros.format(Math.abs(ecart));
    return `${montant} vs période précédente`;
}

// "2026-10-13" → Date locale (new Date("2026-10-13") serait en UTC et peut décaler d'un jour)
export function versDate(iso: string): Date {
    const [annee, mois, jour] = iso.split("-").map(Number);
    return new Date(annee, mois - 1, jour);
}

export function formatDateCourte(iso: string): string {
    return dateCourte.format(versDate(iso));
}

export function formatDateLongue(iso: string): string {
    return dateLongue.format(versDate(iso));
}

export function versIso(date: Date): string {
    const mois = String(date.getMonth() + 1).padStart(2, "0");
    const jour = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${mois}-${jour}`;
}
