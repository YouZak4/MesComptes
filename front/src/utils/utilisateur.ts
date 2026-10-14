import type { Utilisateur } from "@/types/utilisateur.ts";

// « Jean Dupont », ou le nom seul (qui peut être un pseudo) sans prénom
export function nomAffiche(u: Utilisateur): string {
    return u.prenom ? `${u.prenom} ${u.nom}` : u.nom;
}
