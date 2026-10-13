<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { comptesService } from "@/services/comptesService.ts";
import { useNotificationStore } from "@/stores/notification";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import type {
    Categorie,
    Compte,
    Libelle,
    Occurrence,
    Operation,
    TableauDeBord,
    TotalCategorie,
    TypeOperation,
} from "@/types/comptes.ts";
import {
    formatDateCourte,
    formatDateLongue,
    formatEcart,
    formatMontant,
    formatMontantSigne,
    versIso,
} from "@/utils/format.ts";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();

const tableau = ref<TableauDeBord | null>(null);
const comptes = ref<Compte[]>([]);
const categories = ref(new Map<number, Categorie>());
const libelles = ref(new Map<number, Libelle>());
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);
// Aucune période encore définie : on propose de créer la première
const sansPeriode = ref(false);

// Période et compte affichés sont dans l'URL : un rechargement garde la sélection
const periodeId = computed(() => idDepuisQuery(route.query.periode));
const compteId = computed(() => idDepuisQuery(route.query.compte));

function idDepuisQuery(valeur: unknown): number | undefined {
    const id = Number(valeur);
    return Number.isInteger(id) && id > 0 ? id : undefined;
}

function messageErreur(e: any, parDefaut: string): string {
    return e.response?.data?.message || parDefaut;
}

async function chargerReferentiels() {
    const [listeComptes, listeCategories, listeLibelles] = await Promise.all([
        comptesService.getComptes(),
        comptesService.getCategories(),
        comptesService.getLibelles(),
    ]);
    comptes.value = listeComptes;
    categories.value = new Map(listeCategories.map((c) => [c.id, c]));
    libelles.value = new Map(listeLibelles.map((l) => [l.id, l]));
}

async function chargerTableau() {
    isLoading.value = true;
    errorMessage.value = null;
    try {
        tableau.value = await comptesService.getTableauDeBord(
            periodeId.value,
            compteId.value
        );
        sansPeriode.value = false;
    } catch (e: any) {
        tableau.value = null;
        // 422 : l'utilisateur n'a encore aucune période
        sansPeriode.value = e.response?.status === 422;
        if (!sansPeriode.value) {
            errorMessage.value = messageErreur(
                e,
                "Impossible de charger le tableau de bord."
            );
        }
    } finally {
        isLoading.value = false;
    }
}

onMounted(async () => {
    try {
        await chargerReferentiels();
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos comptes."
        );
        isLoading.value = false;
        return;
    }
    await chargerTableau();
});

watch([periodeId, compteId], chargerTableau);

function allerALaPeriode(id: number | null) {
    if (id === null) return;
    router.push({ query: { ...route.query, periode: String(id) } });
}

function choisirCompte(event: Event) {
    const valeur = (event.target as HTMLSelectElement).value;
    const query = { ...route.query };
    if (valeur) query.compte = valeur;
    else delete query.compte;
    router.push({ query });
}

// Les comptes archivés restent consultables, mais seulement s'ils sont déjà sélectionnés
const comptesProposes = computed(() =>
    comptes.value.filter((c) => !c.archive || c.id === compteId.value)
);

const nomsComptes = computed(
    () => new Map(comptes.value.map((c) => [c.id, c.nom]))
);

const depensesParCategorie = computed(() => totauxDuType("DEPENSE"));
const revenusParCategorie = computed(() => totauxDuType("REVENU"));

// Le back renvoie aussi les catégories à zéro sur la période mais présentes sur la précédente (pour l'écart)
function totauxDuType(type: TypeOperation): TotalCategorie[] {
    return (tableau.value?.parCategorie ?? []).filter((t) => t.type === type);
}

function largeurBarre(total: number, liste: TotalCategorie[]): string {
    const max = Math.max(...liste.map((t) => t.total));
    return max > 0 ? `${(total / max) * 100}%` : "0";
}

// Plus de dépenses ou moins de revenus que la période précédente : en rouge
function classeEcart(t: TotalCategorie): string {
    if (t.ecart === 0) return "neutre";
    const hausse = t.ecart > 0;
    return hausse === (t.type === "REVENU") ? "favorable" : "defavorable";
}

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

function details(op: Operation | Occurrence): string {
    const morceaux: string[] = [];
    // La catégorie n'est répétée que si le titre est un libellé
    if (op.libelleId !== null) {
        const categorie = categories.value.get(op.categorieId);
        if (categorie) morceaux.push(categorie.nom);
    }
    // Avec tous les comptes affichés, on précise lequel
    if (compteId.value === undefined && comptes.value.length > 1) {
        const compte = nomsComptes.value.get(op.compteId);
        if (compte) morceaux.push(compte);
    }
    if (op.note) morceaux.push(op.note);
    return morceaux.join(" · ");
}

function classeMontant(type: TypeOperation): string {
    return type === "DEPENSE" ? "depense" : "revenu";
}

// Rouge si négatif, vert si positif, neutre à zéro
function classeSolde(montant: number): string {
    if (montant === 0) return "";
    return montant < 0 ? "depense" : "revenu";
}

const libelleSoldeComptes = computed(() =>
    compteId.value === undefined
        ? "Solde des comptes en fin de période"
        : "Solde du compte en fin de période"
);

// Première période : préremplie avec le mois en cours
const debutPremierePeriode = ref("");
const finPremierePeriode = ref("");
const creationEnCours = ref(false);

function preremplirPremierePeriode() {
    const maintenant = new Date();
    const annee = maintenant.getFullYear();
    const mois = maintenant.getMonth();
    debutPremierePeriode.value = versIso(new Date(annee, mois, 1));
    finPremierePeriode.value = versIso(new Date(annee, mois + 1, 0));
}
preremplirPremierePeriode();

async function creerPremierePeriode() {
    creationEnCours.value = true;
    try {
        await comptesService.creerPeriode(
            debutPremierePeriode.value,
            finPremierePeriode.value
        );
        notification.success("Votre première période est créée.");
        await chargerTableau();
    } catch (e: any) {
        notification.error(messageErreur(e, "Impossible de créer la période."));
    } finally {
        creationEnCours.value = false;
    }
}
</script>

<template>
    <EnTeteComponent />

    <main class="tableau-de-bord">
        <p v-if="isLoading && !tableau" class="message">Chargement...</p>

        <p v-else-if="errorMessage" class="message erreur">
            {{ errorMessage }}
        </p>

        <section v-else-if="sansPeriode" class="carte premiere-periode">
            <h1>Bienvenue sur Mes comptes</h1>
            <p>
                Pour commencer, définissez votre première période : votre « mois
                de compte », par exemple d'un jour de paie au suivant.
            </p>
            <form @submit.prevent="creerPremierePeriode">
                <label>
                    Début
                    <input
                        v-model="debutPremierePeriode"
                        type="date"
                        required
                    />
                </label>
                <label>
                    Fin
                    <input
                        v-model="finPremierePeriode"
                        type="date"
                        :min="debutPremierePeriode"
                        required
                    />
                </label>
                <button
                    type="submit"
                    class="bouton-principal"
                    :disabled="creationEnCours"
                >
                    Créer la période
                </button>
            </form>
        </section>

        <template v-else-if="tableau">
            <!-- Sélecteurs de période et de compte -->
            <div class="barre-selection">
                <div class="selecteur-periode">
                    <button
                        type="button"
                        class="fleche"
                        aria-label="Période précédente"
                        :disabled="tableau.periodePrecedenteId === null"
                        @click="allerALaPeriode(tableau.periodePrecedenteId)"
                    >
                        ‹
                    </button>
                    <div class="periode">
                        <h1>{{ tableau.periode.nom }}</h1>
                        <p>
                            du
                            {{ formatDateLongue(tableau.periode.dateDebut) }} au
                            {{ formatDateLongue(tableau.periode.dateFin) }}
                        </p>
                    </div>
                    <button
                        type="button"
                        class="fleche"
                        aria-label="Période suivante"
                        :disabled="tableau.periodeSuivanteId === null"
                        @click="allerALaPeriode(tableau.periodeSuivanteId)"
                    >
                        ›
                    </button>
                </div>

                <select
                    v-if="comptesProposes.length > 1"
                    class="selecteur-compte"
                    aria-label="Compte"
                    :value="compteId ?? ''"
                    @change="choisirCompte"
                >
                    <option value="">Tous les comptes</option>
                    <option
                        v-for="compte in comptesProposes"
                        :key="compte.id"
                        :value="compte.id"
                    >
                        {{ compte.nom }}{{ compte.archive ? " (archivé)" : "" }}
                    </option>
                </select>
            </div>

            <!-- Les trois chiffres de la période -->
            <div class="chiffres" :class="{ rechargement: isLoading }">
                <div class="carte chiffre">
                    <span class="intitule">Revenus</span>
                    <span class="valeur revenu">
                        {{ formatMontant(tableau.revenus) }}
                    </span>
                </div>
                <div class="carte chiffre">
                    <span class="intitule">Dépenses</span>
                    <span class="valeur depense">
                        {{ formatMontant(tableau.depenses) }}
                    </span>
                </div>
                <div class="carte chiffre">
                    <span class="intitule">Solde de la période</span>
                    <span class="valeur" :class="classeSolde(tableau.solde)">
                        {{ formatMontant(tableau.solde) }}
                    </span>
                </div>
            </div>

            <div class="colonnes" :class="{ rechargement: isLoading }">
                <!-- Totaux par catégorie -->
                <section class="carte">
                    <h2>Dépenses par catégorie</h2>
                    <p v-if="depensesParCategorie.length === 0" class="vide">
                        Aucune dépense sur la période.
                    </p>
                    <ul v-else class="liste-categories">
                        <li
                            v-for="t in depensesParCategorie"
                            :key="t.categorieId"
                        >
                            <div class="ligne-categorie">
                                <span class="icone">{{ t.icone ?? "•" }}</span>
                                <span class="nom">{{ t.nom }}</span>
                                <span class="montant">
                                    {{ formatMontant(t.total) }}
                                </span>
                            </div>
                            <div class="barre">
                                <div
                                    class="remplissage"
                                    :style="{
                                        width: largeurBarre(
                                            t.total,
                                            depensesParCategorie
                                        ),
                                        background: t.couleur ?? '#45335c',
                                    }"
                                ></div>
                            </div>
                            <span
                                class="ecart"
                                :class="classeEcart(t)"
                                :title="`Période précédente : ${formatMontant(t.totalPrecedent)}`"
                            >
                                {{ formatEcart(t.ecart) }} par rapport à la
                                période précédente
                            </span>
                        </li>
                    </ul>

                    <template v-if="revenusParCategorie.length > 0">
                        <h2>Revenus par catégorie</h2>
                        <ul class="liste-categories">
                            <li
                                v-for="t in revenusParCategorie"
                                :key="t.categorieId"
                            >
                                <div class="ligne-categorie">
                                    <span class="icone">{{
                                        t.icone ?? "•"
                                    }}</span>
                                    <span class="nom">{{ t.nom }}</span>
                                    <span class="montant">
                                        {{ formatMontant(t.total) }}
                                    </span>
                                </div>
                                <div class="barre">
                                    <div
                                        class="remplissage"
                                        :style="{
                                            width: largeurBarre(
                                                t.total,
                                                revenusParCategorie
                                            ),
                                            background: t.couleur ?? '#14a81a',
                                        }"
                                    ></div>
                                </div>
                                <span
                                    class="ecart"
                                    :class="classeEcart(t)"
                                    :title="`Période précédente : ${formatMontant(t.totalPrecedent)}`"
                                >
                                    {{ formatEcart(t.ecart) }} par rapport à la
                                    période précédente
                                </span>
                            </li>
                        </ul>
                    </template>
                </section>

                <div class="colonne-droite">
                    <!-- Récurrences à venir et prévision -->
                    <section class="carte">
                        <h2>À venir sur la période</h2>
                        <p v-if="tableau.aVenir.length === 0" class="vide">
                            Aucune opération récurrente à venir.
                        </p>
                        <ul v-else class="liste-operations">
                            <li
                                v-for="occ in tableau.aVenir"
                                :key="`${occ.recurrenceId}-${occ.date}`"
                            >
                                <span class="icone">{{
                                    icone(occ.categorieId)
                                }}</span>
                                <div class="texte">
                                    <span class="titre">{{ titre(occ) }}</span>
                                    <span class="details">
                                        {{ formatDateCourte(occ.date) }}
                                        <template v-if="details(occ)">
                                            · {{ details(occ) }}
                                        </template>
                                    </span>
                                </div>
                                <span
                                    class="montant"
                                    :class="classeMontant(occ.type)"
                                >
                                    {{
                                        formatMontantSigne(
                                            occ.montant,
                                            occ.type
                                        )
                                    }}
                                </span>
                            </li>
                        </ul>
                        <dl class="previsions">
                            <div>
                                <dt>Solde de la période prévu</dt>
                                <dd :class="classeSolde(tableau.soldePrevu)">
                                    {{ formatMontant(tableau.soldePrevu) }}
                                </dd>
                            </div>
                            <div>
                                <dt>{{ libelleSoldeComptes }}</dt>
                                <dd
                                    :class="
                                        classeSolde(
                                            tableau.soldeComptesFinPeriode
                                        )
                                    "
                                >
                                    {{
                                        formatMontant(
                                            tableau.soldeComptesFinPeriode
                                        )
                                    }}
                                </dd>
                            </div>
                        </dl>
                    </section>

                    <!-- Dernières opérations -->
                    <section class="carte">
                        <h2>Dernières opérations</h2>
                        <p
                            v-if="tableau.dernieresOperations.length === 0"
                            class="vide"
                        >
                            Aucune opération sur la période.
                        </p>
                        <ul v-else class="liste-operations">
                            <li
                                v-for="op in tableau.dernieresOperations"
                                :key="op.id"
                            >
                                <span class="icone">{{
                                    icone(op.categorieId)
                                }}</span>
                                <div class="texte">
                                    <span class="titre">
                                        {{ titre(op) }}
                                        <span
                                            v-if="op.recurrenceId !== null"
                                            class="badge"
                                            title="Générée par une récurrence"
                                            >🔁</span
                                        >
                                    </span>
                                    <span class="details">
                                        {{ formatDateCourte(op.date) }}
                                        <template v-if="details(op)">
                                            · {{ details(op) }}
                                        </template>
                                    </span>
                                </div>
                                <span
                                    class="montant"
                                    :class="classeMontant(op.type)"
                                >
                                    {{
                                        formatMontantSigne(op.montant, op.type)
                                    }}
                                </span>
                            </li>
                        </ul>
                    </section>
                </div>
            </div>
        </template>
    </main>
</template>

<style scoped>
.tableau-de-bord {
    color: #45335c;
    max-width: 1100px;
    margin: 0 auto;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

h1,
h2 {
    margin: 0;
}

h2 {
    font-size: 1.1rem;
    margin-bottom: 0.75rem;
}

h2:not(:first-child) {
    margin-top: 1.5rem;
}

.message {
    text-align: center;
}

.erreur {
    color: #c62727;
}

.carte {
    background: #ffffff;
    border: 1px solid #e3d3f1;
    border-radius: 10px;
    padding: 1.25rem;
    box-shadow: 0 2px 6px rgba(69, 51, 92, 0.08);
}

.vide {
    color: #81758e;
    margin: 0;
}

.depense {
    color: #c62727;
}

.revenu {
    color: #14a81a;
}

/* Rechargement d'une autre période : on garde l'ancien contenu, estompé */
.rechargement {
    opacity: 0.5;
    transition: opacity 0.2s;
}

/* Sélecteurs */
.barre-selection {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 1rem;
}

.selecteur-periode {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}

.periode p {
    margin: 0.25rem 0 0;
    color: #81758e;
}

.fleche {
    width: 2.25rem;
    height: 2.25rem;
    border: 1px solid #45335c;
    border-radius: 50%;
    background: #e3d3f1;
    color: #45335c;
    font-size: 1.4rem;
    line-height: 1;
    cursor: pointer;
}

.fleche:disabled {
    opacity: 0.3;
    cursor: default;
}

.selecteur-compte {
    padding: 0.5rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #45335c;
    font-size: 0.95rem;
}

/* Les trois chiffres */
.chiffres {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
}

.chiffre {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.intitule {
    color: #81758e;
}

.valeur {
    font-size: 1.6rem;
    font-weight: bold;
}

/* Deux colonnes, une seule sur petit écran */
.colonnes {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 1rem;
    align-items: start;
}

.colonne-droite {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

ul {
    list-style: none;
    margin: 0;
    padding: 0;
}

/* Totaux par catégorie */
.liste-categories {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.ligne-categorie {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.ligne-categorie .nom {
    flex: 1;
}

.ligne-categorie .montant {
    font-weight: bold;
}

.barre {
    height: 6px;
    margin: 0.35rem 0 0.25rem;
    border-radius: 3px;
    background: #f1e9f8;
    overflow: hidden;
}

.remplissage {
    height: 100%;
    border-radius: 3px;
}

.ecart {
    font-size: 0.8rem;
}

.ecart.neutre {
    color: #81758e;
}

.ecart.favorable {
    color: #14a81a;
}

.ecart.defavorable {
    color: #c62727;
}

/* Listes d'opérations */
.liste-operations li {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0;
    border-bottom: 1px solid #f1e9f8;
}

.liste-operations li:last-child {
    border-bottom: none;
}

.icone {
    width: 1.5rem;
    text-align: center;
}

.texte {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.titre {
    font-weight: bold;
}

.badge {
    font-size: 0.8rem;
    margin-left: 0.25rem;
}

.details {
    font-size: 0.85rem;
    color: #81758e;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.liste-operations .montant {
    font-weight: bold;
    white-space: nowrap;
}

/* Prévisions */
.previsions {
    margin: 1rem 0 0;
    padding-top: 0.75rem;
    border-top: 2px solid #e3d3f1;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.previsions div {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
}

.previsions dt {
    color: #81758e;
}

.previsions dd {
    margin: 0;
    font-weight: bold;
}

/* Première période */
.premiere-periode {
    max-width: 480px;
    margin: 2rem auto;
    text-align: center;
}

.premiere-periode p {
    margin: 1rem 0 1.25rem;
}

.premiere-periode form {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    align-items: stretch;
    text-align: left;
}

.premiere-periode label {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
}

.premiere-periode input {
    padding: 0.55rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #000000;
    font-size: 0.95rem;
}

.bouton-principal {
    margin-top: 0.5rem;
    padding: 0.6rem 1rem;
    border: none;
    border-radius: 10px;
    background: #45335c;
    color: #ffffff;
    font-size: 1rem;
    cursor: pointer;
}

.bouton-principal:disabled {
    opacity: 0.6;
    cursor: default;
}
</style>
