// Message d'une erreur d'API (DtoApiError côté back), sinon le message par défaut
export function messageErreur(e: any, parDefaut: string): string {
    return e?.response?.data?.message || parDefaut;
}
