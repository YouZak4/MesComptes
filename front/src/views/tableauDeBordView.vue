<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
    ArrowDown,
    ArrowUp,
    CalendarClock,
    ChartPie,
    ChevronDown,
    Receipt,
} from "@lucide/vue";
import { comptesService } from "@/services/comptesService.ts";
import { useReferentiels } from "@/composables/useReferentiels.ts";
import { useSaisieStore } from "@/stores/saisie";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import LigneOperationComponent from "@/components/LigneOperationComponent.vue";
import LigneRepartitionComponent from "@/components/LigneRepartitionComponent.vue";
import PastilleCategorieComponent from "@/components/PastilleCategorieComponent.vue";
import SelecteurPeriodeComponent from "@/components/SelecteurPeriodeComponent.vue";
import type {
    Categorie,
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
    formatPourcentage,
    versIso,
} from "@/utils/format.ts";
import { teinteDe } from "@/utils/categories.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import { idConnu, idDepuisQuery } from "@/utils/routage.ts";

const route = useRoute();
const router = useRouter();
const saisie = useSaisieStore();

const tableau = ref<TableauDeBord | null>(null);
const {
    comptes,
    nomsComptes,
    chargerReferentiels,
    titre,
    categorie,
    details: detailsOperation,
} = useReferentiels();
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

// Période et compte affichés sont dans l'URL : un rechargement garde la sélection ; un compte inconnu est ignoré
const periodeId = computed(() => idDepuisQuery(route.query.periode));
const compteId = computed(() => idConnu(route.query.compte, nomsComptes.value));
// Le compte n'est connu qu'une fois les référentiels chargés : pas de tableau avant
const referentielsCharges = ref(false);

async function chargerTableau() {
    if (!referentielsCharges.value) return;
    isLoading.value = true;
    errorMessage.value = null;
    try {
        tableau.value = await comptesService.getTableauDeBord(
            periodeId.value,
            compteId.value
        );
    } catch (e: any) {
        tableau.value = null;
        // 422 : l'utilisateur n'a encore aucune période, c'est sa première connexion
        if (e.response?.status === 422) {
            router.replace({ name: "bienvenue" });
            return;
        }
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger le tableau de bord."
        );
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
    referentielsCharges.value = true;
    await chargerTableau();
});

// Une nouvelle opération est datée dans la période affichée
watch(
    () => tableau.value?.periode ?? null,
    (p) => saisie.afficherPeriode(p),
    { immediate: true }
);
onUnmounted(() => saisie.oublierPeriode(tableau.value?.periode ?? null));

watch([periodeId, compteId], chargerTableau);
// Une opération saisie depuis la saisie rapide : les totaux changent, et un libellé a pu être créé
watch(
    () => saisie.nbEnregistrees,
    async () => {
        try {
            await chargerReferentiels();
        } catch (e) {
            // Les anciens référentiels restent utilisables
            console.error(e);
        }
        await chargerTableau();
    }
);

function allerALaPeriode(id: number | null) {
    if (id === null) return;
    router.push({ query: { ...route.query, periode: String(id) } });
}

// Sans période dans l'URL, le back renvoie celle d'aujourd'hui
function allerALaPeriodeCourante() {
    const query = { ...route.query };
    delete query.periode;
    router.push({ query });
}

const periodeEnCours = computed(() => {
    if (!tableau.value) return true;
    const aujourdhui = versIso(new Date());
    const { dateDebut, dateFin } = tableau.value.periode;
    return dateDebut <= aujourdhui && aujourdhui <= dateFin;
});

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

// Lien « Tout voir » : l'écran Opérations sur la même période et le même compte
const lienOperations = computed(() => ({
    name: "operations",
    query: {
        ...(tableau.value ? { periode: String(tableau.value.periode.id) } : {}),
        ...(compteId.value !== undefined
            ? { compte: String(compteId.value) }
            : {}),
    },
}));

const depensesParCategorie = computed(() => totauxDuType("DEPENSE"));
const revenusParCategorie = computed(() => totauxDuType("REVENU"));

// Le back renvoie aussi les catégories à zéro sur la période mais présentes sur la précédente (pour l'écart)
function totauxDuType(type: TypeOperation): TotalCategorie[] {
    return (tableau.value?.parCategorie ?? [])
        .filter((t) => t.type === type)
        .sort((a, b) => b.total - a.total);
}

// ── Répartition des dépenses : anneau et légende ──

// Au-delà de 6 catégories, les plus petites sont regroupées en « Autres »
const MAX_SEGMENTS = 6;

interface Segment {
    cle: string;
    nom: string;
    total: number;
    part: number;
    pastille: Pick<Categorie, "icone" | "couleur" | "type">;
    // Absent pour « Autres »
    detail?: TotalCategorie;
    // « Autres » seulement : les petites catégories qu'il regroupe
    regroupees?: Segment[];
}

const repartition = computed<Segment[]>(() => {
    const liste = depensesParCategorie.value;
    const somme = liste.reduce((s, t) => s + t.total, 0);
    const part = (total: number) => (somme > 0 ? total / somme : 0);
    const segment = (t: TotalCategorie): Segment => ({
        cle: String(t.categorieId),
        nom: t.nom,
        total: t.total,
        part: part(t.total),
        pastille: t,
        detail: t,
    });
    if (liste.length <= MAX_SEGMENTS) return liste.map(segment);
    const gardees = liste.slice(0, MAX_SEGMENTS - 1);
    const petites = liste.slice(MAX_SEGMENTS - 1);
    const reste = petites.reduce((s, t) => s + t.total, 0);
    return [
        ...gardees.map(segment),
        {
            cle: "autres",
            nom: "Autres",
            total: Math.round(reste * 100) / 100,
            part: part(reste),
            pastille: { icone: "package", couleur: "#6B7079", type: "DEPENSE" },
            regroupees: petites.map(segment),
        },
    ];
});

// « Autres » déplié dans la légende : le détail des catégories qu'il regroupe
const autresDeplies = ref(false);

const regroupees = computed(
    () => repartition.value.find((s) => s.regroupees)?.regroupees ?? []
);

const totalDepensesCategories = computed(() =>
    depensesParCategorie.value.reduce((s, t) => s + t.total, 0)
);

const fondAnneau = computed(() => {
    let debut = 0;
    const morceaux = repartition.value
        .filter((s) => s.part > 0)
        .map((s) => {
            const fin = debut + s.part * 100;
            const couleur = teinteDe(s.pastille.couleur, "DEPENSE").couleur;
            const morceau = `${couleur} ${debut}% ${fin}%`;
            debut = fin;
            return morceau;
        });
    return morceaux.length > 0
        ? `conic-gradient(${morceaux.join(", ")})`
        : "var(--trait)";
});

const descriptionAnneau = computed(
    () =>
        "Répartition des dépenses : " +
        repartition.value
            .filter((s) => s.part > 0)
            .map((s) => `${s.nom} ${formatPourcentage(s.part)}`)
            .join(", ")
);

// Plus de dépenses ou moins de revenus que la période précédente : défavorable
function classeEcart(t: TotalCategorie): string {
    if (t.ecart === 0) return "";
    const hausse = t.ecart > 0;
    return hausse === (t.type === "REVENU") ? "positif" : "negatif";
}

// ── Listes ──

// Avec tous les comptes affichés, on précise celui de chaque opération
function meta(op: Operation | Occurrence): string {
    const morceaux = [formatDateCourte(op.date)];
    const details = detailsOperation(
        op,
        compteId.value === undefined && comptes.value.length > 1
    );
    if (details) morceaux.push(details);
    return morceaux.join(" · ");
}

// Brique si négatif ; un solde positif reste à l'encre
function classeSolde(montant: number): string {
    return montant < 0 ? "negatif" : "";
}

const libelleSoldeComptes = computed(() =>
    compteId.value === undefined
        ? "Comptes en fin de période"
        : "Compte en fin de période"
);
</script>

<template>
    <EnTeteComponent />

    <main class="page tableau-de-bord">
        <p v-if="isLoading && !tableau" class="message">Chargement...</p>

        <p v-else-if="errorMessage" class="message erreur" role="alert">
            {{ errorMessage }}
        </p>

        <template v-else-if="tableau">
            <!-- Période (titre et dates à gauche), sélecteurs à droite -->
            <div class="en-tete-page">
                <div>
                    <h1 class="titre-page">{{ tableau.periode.nom }}</h1>
                    <p class="sous-titre">
                        du
                        {{ formatDateLongue(tableau.periode.dateDebut) }} au
                        {{ formatDateLongue(tableau.periode.dateFin) }}
                    </p>
                </div>
                <div class="selecteurs">
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
                            {{ compte.nom
                            }}{{ compte.archive ? " (archivé)" : "" }}
                        </option>
                    </select>
                    <SelecteurPeriodeComponent
                        :precedente="tableau.periodePrecedenteId !== null"
                        :suivante="tableau.periodeSuivanteId !== null"
                        :en-cours="periodeEnCours"
                        @precedente="
                            allerALaPeriode(tableau.periodePrecedenteId)
                        "
                        @suivante="allerALaPeriode(tableau.periodeSuivanteId)"
                        @courante="allerALaPeriodeCourante"
                    />
                </div>
            </div>

            <!-- Les trois indicateurs : le solde d'abord, sur fond Forêt -->
            <div class="indicateurs" :class="{ rechargement: isLoading }">
                <div class="carte heros">
                    <span class="intitule">Solde de la période</span>
                    <span
                        class="valeur num"
                        :class="{ 'negatif-sombre': tableau.solde < 0 }"
                    >
                        {{ formatMontant(tableau.solde) }}
                    </span>
                </div>
                <div class="carte indicateur">
                    <span
                        class="pastille-indicateur revenus"
                        aria-hidden="true"
                    >
                        <ArrowUp :size="20" :stroke-width="2" />
                    </span>
                    <div>
                        <span class="intitule">Revenus</span>
                        <span class="valeur num positif">
                            {{ formatMontant(tableau.revenus) }}
                        </span>
                    </div>
                </div>
                <div class="carte indicateur">
                    <span
                        class="pastille-indicateur depenses"
                        aria-hidden="true"
                    >
                        <ArrowDown :size="20" :stroke-width="2" />
                    </span>
                    <div>
                        <span class="intitule">Dépenses</span>
                        <span class="valeur num">
                            {{ formatMontant(tableau.depenses) }}
                        </span>
                    </div>
                </div>
            </div>

            <div class="colonnes" :class="{ rechargement: isLoading }">
                <!-- Répartition par catégorie -->
                <section class="carte colonne categories">
                    <h2 class="titre-carte">Dépenses par catégorie</h2>
                    <p v-if="depensesParCategorie.length === 0" class="vide">
                        <ChartPie aria-hidden="true" />
                        Aucune dépense sur la période.
                    </p>
                    <div v-else class="repartition">
                        <div
                            class="anneau"
                            role="img"
                            :aria-label="descriptionAnneau"
                            :style="{ background: fondAnneau }"
                        >
                            <div class="anneau-centre">
                                <span class="meta">Total</span>
                                <span class="total num">
                                    {{ formatMontant(totalDepensesCategories) }}
                                </span>
                            </div>
                        </div>
                        <ul class="nue legende">
                            <template v-for="s in repartition" :key="s.cle">
                                <!-- « Autres » : un bouton qui déplie les catégories regroupées -->
                                <li v-if="s.regroupees" class="autres">
                                    <button
                                        type="button"
                                        class="ligne-autres"
                                        :aria-expanded="autresDeplies"
                                        aria-controls="detail-autres"
                                        @click="autresDeplies = !autresDeplies"
                                    >
                                        <LigneRepartitionComponent
                                            :nom="s.nom"
                                            :total="s.total"
                                            :part="s.part"
                                            :pastille="s.pastille"
                                            :detail="s.detail"
                                            :avec-ecart="false"
                                        >
                                            <template #meta>
                                                ·
                                                <span class="lien-detail">
                                                    {{
                                                        autresDeplies
                                                            ? "Masquer"
                                                            : "Voir"
                                                    }}
                                                    les
                                                    {{ s.regroupees.length }}
                                                    catégories
                                                </span>
                                            </template>
                                        </LigneRepartitionComponent>
                                        <ChevronDown
                                            class="chevron"
                                            aria-hidden="true"
                                        />
                                    </button>
                                </li>
                                <li v-else>
                                    <LigneRepartitionComponent
                                        :nom="s.nom"
                                        :total="s.total"
                                        :part="s.part"
                                        :pastille="s.pastille"
                                        :detail="s.detail"
                                        :avec-ecart="
                                            tableau.periodePrecedenteId !== null
                                        "
                                    />
                                </li>
                            </template>
                        </ul>
                        <!-- Détail de « Autres » : sous la légende, hors de la rangée de l'anneau -->
                        <ul
                            v-if="regroupees.length > 0"
                            v-show="autresDeplies"
                            id="detail-autres"
                            class="nue legende detail-autres"
                        >
                            <li v-for="r in regroupees" :key="r.cle">
                                <LigneRepartitionComponent
                                    :nom="r.nom"
                                    :total="r.total"
                                    :part="r.part"
                                    :pastille="r.pastille"
                                    :detail="r.detail"
                                    :avec-ecart="
                                        tableau.periodePrecedenteId !== null
                                    "
                                />
                            </li>
                        </ul>
                    </div>

                    <template v-if="revenusParCategorie.length > 0">
                        <h2 class="titre-carte sous-section">
                            Revenus par catégorie
                        </h2>
                        <ul class="nue legende">
                            <li
                                v-for="t in revenusParCategorie"
                                :key="t.categorieId"
                            >
                                <PastilleCategorieComponent :categorie="t" />
                                <div class="texte">
                                    <div class="nom-montant">
                                        <span>{{ t.nom }}</span>
                                        <span class="num positif">
                                            {{ formatMontant(t.total) }}
                                        </span>
                                    </div>
                                    <span
                                        v-if="
                                            tableau.periodePrecedenteId !== null
                                        "
                                        class="meta"
                                    >
                                        <span
                                            class="ecart num"
                                            :class="classeEcart(t)"
                                            :title="`Période précédente : ${formatMontant(t.totalPrecedent)}`"
                                        >
                                            {{ formatEcart(t.ecart) }}
                                        </span>
                                    </span>
                                </div>
                            </li>
                        </ul>
                    </template>
                </section>

                <div class="colonne pile">
                    <!-- Dernières opérations -->
                    <section class="carte">
                        <div class="en-tete-carte">
                            <h2 class="titre-carte">Dernières opérations</h2>
                            <RouterLink :to="lienOperations" class="tout-voir">
                                Tout voir
                            </RouterLink>
                        </div>
                        <p
                            v-if="tableau.dernieresOperations.length === 0"
                            class="vide"
                        >
                            <Receipt aria-hidden="true" />
                            Aucune opération sur la période.
                        </p>
                        <ul v-else class="nue">
                            <LigneOperationComponent
                                v-for="op in tableau.dernieresOperations"
                                :key="op.id"
                                :categorie="categorie(op.categorieId)"
                                :titre="titre(op)"
                                :meta="meta(op)"
                                :montant="op.montant"
                                :type="op.type"
                            >
                                <template
                                    v-if="op.recurrenceId !== null"
                                    #badges
                                >
                                    <span
                                        class="badge"
                                        title="Générée par une récurrence"
                                        >Récurrente</span
                                    >
                                </template>
                            </LigneOperationComponent>
                        </ul>
                    </section>

                    <!-- Récurrences à venir et prévision -->
                    <section class="carte">
                        <h2 class="titre-carte a-venir">
                            À venir sur la période
                        </h2>
                        <p v-if="tableau.aVenir.length === 0" class="vide">
                            <CalendarClock aria-hidden="true" />
                            <span>
                                Aucune opération récurrente à venir.
                                <RouterLink :to="{ name: 'recurrences' }">
                                    Ajouter une récurrence
                                </RouterLink>
                            </span>
                        </p>
                        <ul v-else class="nue">
                            <LigneOperationComponent
                                v-for="occ in tableau.aVenir"
                                :key="`${occ.recurrenceId}-${occ.date}`"
                                :categorie="categorie(occ.categorieId)"
                                :titre="titre(occ)"
                                :meta="meta(occ)"
                                :montant="occ.montant"
                                :type="occ.type"
                            />
                        </ul>
                        <dl class="previsions">
                            <div class="encart">
                                <dt class="meta">Solde prévu</dt>
                                <dd
                                    class="num"
                                    :class="classeSolde(tableau.soldePrevu)"
                                >
                                    {{ formatMontant(tableau.soldePrevu) }}
                                </dd>
                            </div>
                            <div class="encart">
                                <dt class="meta">{{ libelleSoldeComptes }}</dt>
                                <dd
                                    class="num"
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
                </div>
            </div>
        </template>
    </main>
</template>

<style scoped>
.selecteurs {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
}

.selecteur-compte {
    border-color: transparent;
    box-shadow: var(--ombre-carte);
    border-radius: var(--rayon-l);
}

/* Indicateurs */
.indicateurs {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 210px), 1fr));
    gap: 16px;
}

.intitule {
    display: block;
    color: var(--encre-douce);
    font-weight: 600;
}

.valeur {
    display: block;
    font-size: 24px;
    line-height: 1.2;
    font-weight: 800;
}

.heros {
    background: var(--vert-900);
    color: #ffffff;
    box-shadow: none;
}

.heros .intitule {
    color: #b9d6c6;
}

.heros .valeur {
    margin-top: 6px;
    font-size: 32px;
    line-height: 1.1;
    letter-spacing: -0.02em;
}

.heros .negatif-sombre {
    color: var(--negatif-sur-sombre);
}

.indicateur {
    display: flex;
    align-items: center;
    gap: 14px;
}

.pastille-indicateur {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 44px;
    height: 44px;
    border-radius: var(--rayon-l);
}

.pastille-indicateur.revenus {
    background: var(--vert-100);
    color: var(--vert-700);
}

.pastille-indicateur.depenses {
    background: var(--negatif-fond);
    color: var(--negatif);
}

/* Deux colonnes souples, l'une sous l'autre sous ≈ 860 px */
.colonnes {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 16px;
}

.colonne {
    flex: 1 1 400px;
    min-width: 0;
}

.pile {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.titre-carte {
    margin-bottom: 14px;
}

.en-tete-carte .titre-carte {
    margin-bottom: 0;
}

.tout-voir {
    font-size: 13px;
}

.sous-section {
    margin-top: 24px;
}

.a-venir {
    margin-bottom: 4px;
}

/* Répartition : l'anneau est centré sur la légende repliée seule. Le détail de « Autres »
   occupe sa propre rangée en dessous, donc l'anneau ne bouge pas quand on le déplie. */
.categories {
    container-type: inline-size;
}

.repartition {
    display: grid;
    grid-template-columns: 140px minmax(0, 1fr);
    grid-template-areas:
        "anneau legende"
        ". detail";
    align-items: center;
    column-gap: 24px;
}

.repartition > .anneau {
    grid-area: anneau;
}

.repartition > .legende {
    grid-area: legende;
}

.repartition > .detail-autres {
    grid-area: detail;
    margin-top: 10px;
}

/* Trop étroit pour l'anneau (140 px) et la légende (200 px) côte à côte : l'un sous l'autre */
@container (max-width: 363px) {
    .repartition {
        grid-template-columns: minmax(0, 1fr);
        grid-template-areas:
            "anneau"
            "legende"
            "detail";
    }

    .repartition > .legende {
        margin-top: 24px;
    }
}

.anneau {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 140px;
    height: 140px;
    border-radius: 50%;
}

.anneau-centre {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: var(--blanc);
}

.anneau-centre .meta {
    font-weight: 600;
}

.anneau-centre .total {
    font-size: 17px;
    font-weight: 800;
}

.legende {
    flex: 1 1 200px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    min-width: 0;
}

.legende li {
    display: flex;
    align-items: center;
    gap: 12px;
}

.legende .texte {
    flex: 1;
    min-width: 0;
}

.nom-montant {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-weight: 700;
}

.nom-montant .num {
    white-space: nowrap;
}

.ecart {
    font-weight: 600;
}

/* « Autres » : toute la ligne est cliquable, un chevron et « Voir les N catégories » l'annoncent */
.legende li.autres {
    flex-direction: column;
    align-items: stretch;
}

.ligne-autres {
    display: flex;
    align-items: center;
    gap: 12px;
    width: calc(100% + 16px);
    margin: -8px;
    padding: 8px;
    border: none;
    border-radius: var(--rayon-m);
    background: none;
    color: inherit;
    font: inherit;
    text-align: left;
    cursor: pointer;
}

.ligne-autres:hover {
    background: var(--vert-50);
}

.ligne-autres:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px #1b5c4226;
}

.lien-detail {
    font-weight: 700;
    color: var(--vert-700);
}

.ligne-autres:hover .lien-detail {
    text-decoration: underline;
}

.chevron {
    flex: none;
    width: 18px;
    height: 18px;
    color: var(--vert-700);
    transition: transform 0.2s;
}

.ligne-autres[aria-expanded="true"] .chevron {
    transform: rotate(180deg);
}

/* Détail : en retrait sous « Autres », rattaché par un filet */
.detail-autres {
    gap: 12px;
    margin-left: 18px;
    padding-left: 18px;
    border-left: 2px solid var(--trait);
}

@media (prefers-reduced-motion: reduce) {
    .chevron {
        transition: none;
    }
}

/* Prévisions */
.previsions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin: 14px 0 0;
}

.previsions .meta {
    font-weight: 600;
}

.previsions dd {
    margin: 0;
    font-weight: 800;
}

@media (max-width: 640px) {
    .previsions {
        grid-template-columns: 1fr;
    }
}
</style>
