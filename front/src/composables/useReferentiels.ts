import { computed, ref } from "vue";
import { comptesService } from "@/services/comptesService.ts";
import type {
    Categorie,
    Compte,
    Libelle,
    Occurrence,
    Operation,
} from "@/types/comptes.ts";

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
    function titre(op: Operation | Occurrence): string {
        if (op.libelleId !== null) {
            const libelle = libelles.value.get(op.libelleId);
            if (libelle) return libelle.nom;
        }
        return categories.value.get(op.categorieId)?.nom ?? "Opération";
    }

    function icone(categorieId: number): string {
        return categories.value.get(categorieId)?.icone ?? "•";
    }

    // Catégorie (seulement si le titre est un libellé), compte (si demandé) et note
    function details(op: Operation | Occurrence, avecCompte: boolean): string {
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
        icone,
        details,
    };
}
