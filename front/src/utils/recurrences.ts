import type { Frequence } from "@/types/comptes.ts";

const nomMois = new Intl.DateTimeFormat("fr-FR", { month: "long" });

function jourEnTexte(jour: number): string {
    return jour === 1 ? "1er" : String(jour);
}

// « Le 5 de chaque mois », « Chaque année le 12 mars » (le mois est celui de la date de début)
export function decrireCalendrier(
    frequence: Frequence,
    jour: number,
    dateDebut: string
): string {
    if (frequence === "MENSUELLE") {
        return `Le ${jourEnTexte(jour)} de chaque mois`;
    }
    const mois = Number(dateDebut.split("-")[1]);
    return `Chaque année le ${jourEnTexte(jour)} ${nomMois.format(new Date(2000, mois - 1, 1))}`;
}

// Jour du mois d'une date ISO "AAAA-MM-JJ"
export function jourDe(iso: string): number {
    return Number(iso.split("-")[2]);
}
