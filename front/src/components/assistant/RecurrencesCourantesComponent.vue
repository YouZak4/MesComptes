<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { Check, CircleAlert } from "@lucide/vue";
import PastilleCategorieComponent from "@/components/PastilleCategorieComponent.vue";
import { vFocus } from "@/directives/focus.ts";
import { comptesService } from "@/services/comptesService.ts";
import type { Categorie, Compte, Libelle, Periode } from "@/types/comptes.ts";
import { formatDateLongue } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";

const props = defineProps<{
    // Les récurrences partent du début de cette période
    periode: Periode;
}>();

const emit = defineEmits<{
    terminee: [nbCreees: number];
}>();

// Libellés fournis proposés, avec un jour du mois habituel (modifiable)
// null : le jour de début de la période (la paie, si la période va d'une paie à l'autre)
const SUGGESTIONS: { nom: string; jour: number | null }[] = [
    { nom: "Salaire", jour: null },
    { nom: "CAF", jour: 5 },
    { nom: "Loyer", jour: 5 },
    { nom: "Crédit immobilier", jour: 5 },
    { nom: "Électricité", jour: 10 },
    { nom: "Gaz", jour: 10 },
    { nom: "Box internet", jour: 15 },
    { nom: "Forfait mobile", jour: 15 },
    { nom: "Assurance habitation", jour: 10 },
    { nom: "Assurance auto", jour: 10 },
    { nom: "Mutuelle", jour: 10 },
    { nom: "Prêt auto", jour: 5 },
    { nom: "Netflix", jour: 20 },
    { nom: "Spotify", jour: 20 },
    { nom: "Salle de sport", jour: 1 },
];

interface Ligne {
    libelle: Libelle;
    categorie: Categorie;
    cochee: boolean;
    montantTexte: string;
    jour: number;
    creee: boolean;
    erreur: string | null;
}

const lignes = ref<Ligne[]>([]);
const comptes = ref<Compte[]>([]);
const compteId = ref<number | null>(null);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

function normaliser(texte: string): string {
    return texte
        .normalize("NFD")
        .replace(/\p{Diacritic}/gu, "")
        .toLowerCase();
}

function jourDe(iso: string): number {
    return Number(iso.slice(8, 10));
}

onMounted(async () => {
    try {
        const [listeComptes, categories, libelles] = await Promise.all([
            comptesService.getComptes(),
            comptesService.getCategories(),
            comptesService.getLibelles(),
        ]);
        comptes.value = listeComptes.filter((c) => !c.archive);
        compteId.value =
            (comptes.value.find((c) => c.dernierUtilise) ?? comptes.value[0])
                ?.id ?? null;

        const parId = new Map(categories.map((c) => [c.id, c]));
        const proposes = new Map(
            libelles
                .filter((l) => !l.masque && !l.archive)
                .map((l) => [normaliser(l.nom), l])
        );
        // Un libellé masqué (ou de catégorie masquée) n'est pas proposé
        lignes.value = SUGGESTIONS.flatMap((s) => {
            const libelle = proposes.get(normaliser(s.nom));
            const categorie = libelle && parId.get(libelle.categorieId);
            if (!libelle || !categorie || categorie.masquee) return [];
            return [
                {
                    libelle,
                    categorie,
                    cochee: false,
                    montantTexte: "",
                    jour: s.jour ?? jourDe(props.periode.dateDebut),
                    creee: false,
                    erreur: null,
                },
            ];
        });
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger les récurrences proposées."
        );
    } finally {
        isLoading.value = false;
    }
});

const revenus = computed(() =>
    lignes.value.filter((l) => l.categorie.type === "REVENU")
);
const depenses = computed(() =>
    lignes.value.filter((l) => l.categorie.type === "DEPENSE")
);

const aCreer = computed(() => lignes.value.filter((l) => l.cochee && !l.creee));

function lireMontant(texte: string): number | null {
    const nettoye = texte.replace(/\s/g, "").replace(",", ".");
    if (!/^\d+(\.\d{1,2})?$/.test(nettoye)) return null;
    const montant = Number(nettoye);
    return montant > 0 ? montant : null;
}

const creationEnCours = ref(false);
const erreurFormulaire = ref<string | null>(null);

async function creer() {
    if (creationEnCours.value) return;
    erreurFormulaire.value = null;
    if (compteId.value === null) {
        erreurFormulaire.value = "Choisissez le compte de ces récurrences.";
        return;
    }

    // On vérifie tout avant d'envoyer quoi que ce soit
    let valide = true;
    for (const ligne of aCreer.value) {
        ligne.erreur = null;
        if (lireMontant(ligne.montantTexte) === null) {
            ligne.erreur = "Montant positif, deux décimales au plus.";
            valide = false;
        } else if (!(ligne.jour >= 1 && ligne.jour <= 31)) {
            ligne.erreur = "Jour entre 1 et 31.";
            valide = false;
        }
    }
    if (!valide) return;

    creationEnCours.value = true;
    let nbCreees = 0;
    for (const ligne of aCreer.value) {
        try {
            await comptesService.creerRecurrence({
                compteId: compteId.value,
                montant: lireMontant(ligne.montantTexte)!,
                categorieId: ligne.categorie.id,
                libelleId: ligne.libelle.id,
                note: null,
                frequence: "MENSUELLE",
                jour: ligne.jour,
                dateDebut: props.periode.dateDebut,
                dateFin: null,
            });
            ligne.creee = true;
            nbCreees++;
        } catch (e: any) {
            ligne.erreur = messageErreur(e, "Impossible de la créer.");
        }
    }
    creationEnCours.value = false;

    if (aCreer.value.length > 0) {
        // Certaines ont échoué : elles restent cochées pour réessayer
        erreurFormulaire.value =
            nbCreees > 0
                ? `${nbCreees} récurrence(s) créée(s), mais pas toutes : corrigez les lignes en erreur.`
                : "Aucune récurrence n'a pu être créée.";
        return;
    }
    emit("terminee", nbCreees);
}
</script>

<template>
    <p v-if="isLoading" class="message">Chargement...</p>
    <p v-else-if="errorMessage" class="message erreur">{{ errorMessage }}</p>
    <!-- Atteint par l'URL après avoir retiré tous ses comptes -->
    <p v-else-if="comptes.length === 0" class="message erreur">
        Vous n'avez aucun compte : ajoutez-en un à l'étape précédente.
    </p>

    <section v-else class="carte">
        <h2>Vos dépenses et revenus réguliers</h2>
        <p class="aide">
            Cochez ce qui revient chaque mois et indiquez le montant et le jour.
            Chaque récurrence ajoute l'opération d'elle-même à sa date ; celles
            déjà passées depuis le
            {{ formatDateLongue(periode.dateDebut) }} (début de la période) sont
            ajoutées tout de suite. Vous pourrez les modifier ou en ajouter
            d'autres dans « Récurrences ».
        </p>

        <form class="recurrences" @submit.prevent="creer">
            <label v-if="comptes.length > 1" class="champ compte">
                Compte
                <select v-model="compteId" required>
                    <option v-for="c in comptes" :key="c.id" :value="c.id">
                        {{ c.nom }}
                    </option>
                </select>
            </label>

            <template
                v-for="groupe in [
                    { titre: 'Revenus', liste: revenus },
                    { titre: 'Dépenses', liste: depenses },
                ]"
                :key="groupe.titre"
            >
                <h3 class="titre-groupe" v-if="groupe.liste.length > 0">
                    {{ groupe.titre }}
                </h3>
                <ul class="liste">
                    <li
                        v-for="ligne in groupe.liste"
                        :key="ligne.libelle.id"
                        :class="{ cochee: ligne.cochee }"
                    >
                        <label class="case">
                            <input
                                v-model="ligne.cochee"
                                type="checkbox"
                                :disabled="ligne.creee"
                            />
                            <PastilleCategorieComponent
                                :categorie="ligne.categorie"
                            />
                            <span class="texte">
                                <span class="titre">{{
                                    ligne.libelle.nom
                                }}</span>
                                <span class="details">{{
                                    ligne.categorie.nom
                                }}</span>
                            </span>
                        </label>

                        <span v-if="ligne.creee" class="badge">
                            <Check aria-hidden="true" />
                            Créée
                        </span>
                        <div v-else-if="ligne.cochee" class="saisie">
                            <label class="champ">
                                Montant (€)
                                <input
                                    v-focus
                                    v-model="ligne.montantTexte"
                                    type="text"
                                    inputmode="decimal"
                                    class="montant"
                                    :aria-label="`Montant : ${ligne.libelle.nom}`"
                                    required
                                />
                            </label>
                            <label class="champ">
                                Le
                                <input
                                    v-model.number="ligne.jour"
                                    type="number"
                                    min="1"
                                    max="31"
                                    class="jour"
                                    :aria-label="`Jour du mois : ${ligne.libelle.nom}`"
                                    required
                                />
                            </label>
                        </div>
                        <p v-if="ligne.erreur" class="erreur" role="alert">
                            <CircleAlert aria-hidden="true" />
                            {{ ligne.erreur }}
                        </p>
                    </li>
                </ul>
            </template>

            <p v-if="lignes.length === 0" class="vide">
                Aucune récurrence à proposer : vous avez masqué ces libellés.
            </p>

            <p v-if="erreurFormulaire" class="erreur" role="alert">
                <CircleAlert aria-hidden="true" />
                {{ erreurFormulaire }}
            </p>

            <div class="actions fin">
                <button
                    type="button"
                    class="btn btn-secondaire"
                    :disabled="creationEnCours"
                    @click="
                        emit('terminee', lignes.filter((l) => l.creee).length)
                    "
                >
                    {{ aCreer.length > 0 ? "Passer" : "Terminer" }}
                </button>
                <button
                    v-if="aCreer.length > 0"
                    type="submit"
                    class="btn btn-secondaire"
                    :disabled="creationEnCours"
                >
                    Créer
                    {{
                        aCreer.length === 1
                            ? "la récurrence"
                            : `les ${aCreer.length} récurrences`
                    }}
                    et terminer
                </button>
            </div>
        </form>
    </section>
</template>

<style scoped>
.recurrences {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 12px;
}

.compte {
    max-width: 320px;
}

.titre-groupe {
    margin: 12px 0 0;
    font-size: 14px;
    font-weight: 800;
}

.case {
    flex: 1 1 220px;
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 44px;
    cursor: pointer;
}

.saisie {
    display: flex;
    gap: 8px;
}

.montant {
    width: 7.5rem;
    font-variant-numeric: tabular-nums;
}

.jour {
    width: 5rem;
}

.recurrences .liste > li.cochee {
    background: var(--vert-50);
}

.liste .erreur {
    flex-basis: 100%;
}

.fin {
    justify-content: flex-end;
    gap: 8px !important;
    margin-top: 8px;
}
</style>
