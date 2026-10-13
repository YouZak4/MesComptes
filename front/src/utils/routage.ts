// Id lu dans un paramètre d'URL (?periode=12) ; absent ou invalide : undefined
export function idDepuisQuery(valeur: unknown): number | undefined {
    const id = Number(valeur);
    return Number.isInteger(id) && id > 0 ? id : undefined;
}
