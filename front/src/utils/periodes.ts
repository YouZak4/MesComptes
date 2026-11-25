import { versDate, versIso } from "@/utils/format.ts";

/**
 * Fin d'une période qui commence le jour du salaire : la veille du même jour,
 * le mois suivant (« 2026-09-25 » → « 2026-10-24 »). Une période couvre ainsi
 * un mois de salaire, et la suivante commence le jour du prochain.
 *
 * Même calcul que le back (`debut.plusMonths(1).minusDays(1)`) : un jour qui
 * n'existe pas le mois suivant est ramené au dernier jour de ce mois
 * (« 2026-01-31 » → 28 février → « 2026-02-27 »).
 */
export function finDePeriode(debutIso: string): string {
    const debut = versDate(debutIso);
    const dernierJourMoisSuivant = new Date(
        debut.getFullYear(),
        debut.getMonth() + 2,
        0
    ).getDate();
    const memeJourMoisSuivant = new Date(
        debut.getFullYear(),
        debut.getMonth() + 1,
        Math.min(debut.getDate(), dernierJourMoisSuivant)
    );
    memeJourMoisSuivant.setDate(memeJourMoisSuivant.getDate() - 1);
    return versIso(memeJourMoisSuivant);
}
