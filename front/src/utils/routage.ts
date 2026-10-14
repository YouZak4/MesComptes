// Id lu dans un paramètre d'URL (?periode=12) ; absent ou invalide : undefined
export function idDepuisQuery(valeur: unknown): number | undefined {
    const id = Number(valeur);
    return Number.isInteger(id) && id > 0 ? id : undefined;
}

// Id lu dans l'URL, gardé seulement s'il désigne un élément connu : un vieux lien peut viser
// un élément supprimé depuis, que le back refuserait en 404. Tant que rien n'est chargé : undefined
export function idConnu(
    valeur: unknown,
    connus: { has(id: number): boolean }
): number | undefined {
    const id = idDepuisQuery(valeur);
    return id !== undefined && connus.has(id) ? id : undefined;
}
