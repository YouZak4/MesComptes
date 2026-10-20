import { computed, ref } from "vue";
import { comptesService } from "@/services/comptesService.ts";
import type { Categorie, Compte, Libelle, Operation } from "@/types/comptes.ts";

// Opération, occurrence à venir ou récurrence : tout ce qui porte un compte, une catégorie, un libellé et une note
type Affichable = Pick<
    Operation,
    "compteId" | "categorieId" | "libelleId" | "note"
>;

// Comptes, catégories et libellés de l'utilisateur, pour afficher des opérations qui ne portent que des ids
export function useReferentiels() {
    const comptes = ref<Compte[]>([]);
    const categories = ref(new Map<number, Categorie>());
    const libelles = ref(new Map<number, Libelle>());

    async function chargerReferentiels() {
        const [listeComptes, listeCategories, listeLibelles] =
            await Promise.all([
                comptesService.getComptes(),
                comptesService.getCategories(),
                comptesService.getLibelles(),
            ]);
        comptes.value = listeComptes;
        categories.value = new Map(listeCategories.map((c) => [c.id, c]));
        libelles.value = new Map(listeLibelles.map((l) => [l.id, l]));
    }

    const nomsComptes = computed(
        () => new Map(comptes.value.map((c) => [c.id, c.nom]))
    );

    // Le libellé s'il y en a un, sinon la catégorie
    function titre(op: Affichable): string {
        if (op.libelleId !== null) {
            const libelle = libelles.value.get(op.libelleId);
            if (libelle) return libelle.nom;
        }
        return categories.value.get(op.categorieId)?.nom ?? "Opération";
    }

    // Pour la pastille (icône et couleurs) d'une opération
    function categorie(categorieId: number): Categorie | undefined {
        return categories.value.get(categorieId);
    }

    // Catégorie (seulement si le titre est un libellé), compte (si demandé) et note
    function details(op: Affichable, avecCompte: boolean): string {
        const morceaux: string[] = [];
        if (op.libelleId !== null && libelles.value.has(op.libelleId)) {
            const categorie = categories.value.get(op.categorieId);
            if (categorie) morceaux.push(categorie.nom);
        }
        if (avecCompte) {
            const compte = nomsComptes.value.get(op.compteId);
            if (compte) morceaux.push(compte);
        }
        if (op.note) morceaux.push(op.note);
        return morceaux.join(" · ");
    }

    return {
        comptes,
        categories,
        libelles,
        nomsComptes,
        chargerReferentiels,
        titre,
        categorie,
        details,
    };
}
