<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { comptesService } from "@/services/comptesService.ts";
import { useReferentiels } from "@/composables/useReferentiels.ts";
import { useNotificationStore } from "@/stores/notification";
import { useSaisieStore } from "@/stores/saisie";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import type { Operation, Periode, TypeOperation } from "@/types/comptes.ts";
import {
    formatDateLongue,
    formatMontant,
    formatMontantSigne,
    versIso,
} from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import { idDepuisQuery } from "@/utils/routage.ts";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();
const saisie = useSaisieStore();

const {
    comptes,
    categories,
    libelles,
    chargerReferentiels,
    titre,
    icone,
    details: detailsOperation,
} = useReferentiels();

const periodes = ref<Periode[]>([]);
const operations = ref<Operation[]>([]);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

// Filtres dans l'URL : un rechargement ou un lien les garde
const compteId = computed(() => idDepuisQuery(route.query.compte));
const categorieId = computed(() => idDepuisQuery(route.query.categorie));
const libelleId = computed(() => idDepuisQuery(route.query.libelle));
const recherche = computed(() =>
    typeof route.query.q === "string" ? route.query.q : ""
);

// ── Période ──

// Sans période dans l'URL : celle qui contient aujourd'hui, sinon la dernière
const periode = computed<Periode | null>(() => {
    const id = idDepuisQuery(route.query.periode);
    const demandee = periodes.value.find((p) => p.id === id);
    if (demandee) return demandee;
    const aujourdhui = versIso(new Date());
    return (
        periodes.value.find(
            (p) => p.dateDebut <= aujourdhui && aujourdhui <= p.dateFin
        ) ??
        periodes.value.at(-1) ??
        null
    );
});

// Les périodes sont triées par date de début
const indexPeriode = computed(() =>
    periode.value ? periodes.value.indexOf(periode.value) : -1
);
const periodePrecedente = computed(
    () => periodes.value[indexPeriode.value - 1] ?? null
);
const periodeSuivante = computed(() =>
    indexPeriode.value >= 0
        ? (periodes.value[indexPeriode.value + 1] ?? null)
        : null
);

function allerALaPeriode(p: Periode | null) {
    if (p) changerFiltre("periode", String(p.id));
}

// ── Chargement ──

async function chargerOperations() {
    if (!periode.value) {
        operations.value = [];
        return;
    }
    isLoading.value = true;
    errorMessage.value = null;
    try {
        operations.value = await comptesService.getOperations({
            periodeId: periode.value.id,
            compteId: compteId.value,
            categorieId: categorieId.value,
            libelleId: libelleId.value,
            q: recherche.value || undefined,
        });
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger les opérations."
        );
    } finally {
        isLoading.value = false;
    }
}

// Un filtre qui ne désigne plus rien (élément supprimé depuis, vieux lien) est retiré de l'URL,
// sinon le back répond 404 et la liste ne s'affiche pas. Rend true si l'URL a changé.
async function retirerFiltresInconnus(): Promise<boolean> {
    const query = { ...route.query };
    if (
        compteId.value !== undefined &&
        !comptes.value.some((c) => c.id === compteId.value)
    )
        delete query.compte;
    if (
        categorieId.value !== undefined &&
        !categories.value.has(categorieId.value)
    )
        delete query.categorie;
    if (libelleId.value !== undefined && !libelles.value.has(libelleId.value))
        delete query.libelle;
    if (Object.keys(query).length === Object.keys(route.query).length)
        return false;
    await router.replace({ query });
    return true;
}

async function toutCharger() {
    isLoading.value = true;
    try {
        const [listePeriodes] = await Promise.all([
            comptesService.getPeriodes(),
            chargerReferentiels(),
        ]);
        periodes.value = listePeriodes;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos comptes."
        );
        isLoading.value = false;
        return;
    }
    // Si l'URL change, le watch des filtres recharge la liste
    if (await retirerFiltresInconnus()) return;
    await chargerOperations();
}

onMounted(toutCharger);

// Sur l'id de la période : recharger la liste des périodes ne doit pas relancer la recherche
watch(
    [() => periode.value?.id, compteId, categorieId, libelleId, recherche],
    () => chargerOperations()
);
// Une opération saisie ou modifiée : un libellé ou une période a pu être créé
watch(() => saisie.nbEnregistrees, toutCharger);

// ── Filtres ──

function changerFiltre(cle: string, valeur: string) {
    const query = { ...route.query };
    if (valeur) query[cle] = valeur;
    else delete query[cle];
    router.replace({ query });
}

function surChoix(cle: string, event: Event) {
    const valeur = (event.target as HTMLSelectElement).value;
    // Changer de catégorie peut rendre le libellé choisi incohérent : on le retire
    if (cle === "categorie" && libelleId.value !== undefined) {
        const libelle = libelles.value.get(libelleId.value);
        if (libelle && String(libelle.categorieId) !== valeur) {
            const query = { ...route.query };
            delete query.libelle;
            if (valeur) query.categorie = valeur;
            else delete query.categorie;
            router.replace({ query });
            return;
        }
    }
    changerFiltre(cle, valeur);
}

// La recherche part 300 ms après la dernière frappe
const texteRecherche = ref(recherche.value);
let minuterieRecherche: ReturnType<typeof setTimeout> | undefined;
function surRecherche() {
    clearTimeout(minuterieRecherche);
    minuterieRecherche = setTimeout(
        () => changerFiltre("q", texteRecherche.value.trim()),
        300
    );
}
watch(recherche, (q) => {
    if (q !== texteRecherche.value.trim()) texteRecherche.value = q;
});

const filtresActifs = computed(
    () =>
        compteId.value !== undefined ||
        categorieId.value !== undefined ||
        libelleId.value !== undefined ||
        recherche.value !== ""
);

function effacerFiltres() {
    clearTimeout(minuterieRecherche);
    texteRecherche.value = "";
    const query = periode.value ? { periode: String(periode.value.id) } : {};
    router.replace({ query });
}

// Les éléments archivés ou masqués restent proposés s'ils sont le filtre en cours
const comptesProposes = computed(() =>
    comptes.value.filter((c) => !c.archive || c.id === compteId.value)
);

const categoriesProposees = computed(() =>
    [...categories.value.values()]
        .filter((c) => (!c.masquee && !c.archive) || c.id === categorieId.value)
        .sort(
            (a, b) => a.type.localeCompare(b.type) || a.nom.localeCompare(b.nom)
        )
);

const libellesProposes = computed(() =>
    [...libelles.value.values()]
        .filter(
            (l) =>
                ((!l.masque && !l.archive) || l.id === libelleId.value) &&
                (categorieId.value === undefined ||
                    l.categorieId === categorieId.value)
        )
        .sort((a, b) => a.nom.localeCompare(b.nom))
);

// ── Liste ──

// Opérations regroupées par jour, du plus récent au plus ancien (ordre du back)
const parJour = computed(() => {
    const groupes: { date: string; operations: Operation[] }[] = [];
    for (const op of operations.value) {
        const dernier = groupes.at(-1);
        if (dernier?.date === op.date) dernier.operations.push(op);
        else groupes.push({ date: op.date, operations: [op] });
    }
    return groupes;
});

const totaux = computed(() => {
    let revenus = 0;
    let depenses = 0;
    for (const op of operations.value) {
        if (op.type === "REVENU") revenus += op.montant;
        else depenses += op.montant;
    }
    // Arrondi au centime : les additions de décimaux en JavaScript ne sont pas exactes
    const arrondi = (n: number) => Math.round(n * 100) / 100;
    return { revenus: arrondi(revenus), depenses: arrondi(depenses) };
});

function details(op: Operation): string {
    return detailsOperation(
        op,
        compteId.value === undefined && comptes.value.length > 1
    );
}

function classeMontant(type: TypeOperation): string {
    return type === "DEPENSE" ? "depense" : "revenu";
}

// ── Actions ──

function modifier(op: Operation) {
    const libelle =
        op.libelleId !== null
            ? (libelles.value.get(op.libelleId) ?? null)
            : null;
    saisie.modifier(op, libelle);
}

const enCours = ref<number | null>(null);
// Suppression en deux temps, sans boîte de dialogue : le premier clic demande confirmation
const aSupprimer = ref<number | null>(null);

async function supprimer(op: Operation) {
    enCours.value = op.id;
    try {
        await comptesService.supprimerOperation(op.id);
        operations.value = operations.value.filter((o) => o.id !== op.id);
        notification.success("Opération supprimée.");
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de supprimer l'opération.")
        );
    } finally {
        enCours.value = null;
        aSupprimer.value = null;
    }
}

async function repeter(op: Operation) {
    enCours.value = op.id;
    try {
        await comptesService.repeterOperation(op.id);
        notification.success(
            `« ${titre(op)} » reviendra chaque mois, le ${Number(op.date.slice(8))}.`
        );
        // Les occurrences déjà échues depuis sa date ont pu être créées
        await chargerOperations();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de créer la récurrence.")
        );
    } finally {
        enCours.value = null;
    }
}
</script>

<template>
    <EnTeteComponent />

    <main class="operations">
        <p v-if="isLoading && periodes.length === 0" class="message">
            Chargement...
        </p>

        <p
            v-else-if="errorMessage && periodes.length === 0"
            class="message erreur"
        >
            {{ errorMessage }}
        </p>

        <section v-else-if="!periode" class="carte message">
            Aucune période n'est encore définie.
            <RouterLink :to="{ name: 'accueil' }">
                Créez-la depuis le tableau de bord.
            </RouterLink>
        </section>

        <template v-else>
            <div class="selecteur-periode">
                <button
                    type="button"
                    class="fleche"
                    aria-label="Période précédente"
                    :disabled="periodePrecedente === null"
                    @click="allerALaPeriode(periodePrecedente)"
                >
                    ‹
                </button>
                <div class="periode">
                    <h1>Opérations · {{ periode.nom }}</h1>
                    <p>
                        du {{ formatDateLongue(periode.dateDebut) }} au
                        {{ formatDateLongue(periode.dateFin) }}
                    </p>
                </div>
                <button
                    type="button"
                    class="fleche"
                    aria-label="Période suivante"
                    :disabled="periodeSuivante === null"
                    @click="allerALaPeriode(periodeSuivante)"
                >
                    ›
                </button>
            </div>

            <!-- Filtres -->
            <div class="carte filtres">
                <input
                    v-model="texteRecherche"
                    type="search"
                    class="recherche"
                    placeholder="Rechercher un libellé ou une note"
                    aria-label="Rechercher"
                    @input="surRecherche"
                />
                <select
                    v-if="comptesProposes.length > 1"
                    aria-label="Compte"
                    :value="compteId ?? ''"
                    @change="surChoix('compte', $event)"
                >
                    <option value="">Tous les comptes</option>
                    <option
                        v-for="c in comptesProposes"
                        :key="c.id"
                        :value="c.id"
                    >
                        {{ c.nom }}{{ c.archive ? " (archivé)" : "" }}
                    </option>
                </select>
                <select
                    aria-label="Catégorie"
                    :value="categorieId ?? ''"
                    @change="surChoix('categorie', $event)"
                >
                    <option value="">Toutes les catégories</option>
                    <option
                        v-for="c in categoriesProposees"
                        :key="c.id"
                        :value="c.id"
                    >
                        {{ c.icone }} {{ c.nom }}
                    </option>
                </select>
                <select
                    aria-label="Libellé"
                    :value="libelleId ?? ''"
                    @change="surChoix('libelle', $event)"
                >
                    <option value="">Tous les libellés</option>
                    <option
                        v-for="l in libellesProposes"
                        :key="l.id"
                        :value="l.id"
                    >
                        {{ l.nom }}
                    </option>
                </select>
                <button
                    v-if="filtresActifs"
                    type="button"
                    class="bouton-lien"
                    @click="effacerFiltres"
                >
                    Effacer les filtres
                </button>
            </div>

            <p v-if="errorMessage" class="message erreur">
                {{ errorMessage }}
            </p>

            <!-- Résumé de ce qui est affiché -->
            <p class="resume">
                {{ operations.length }}
                {{ operations.length > 1 ? "opérations" : "opération" }}
                <template v-if="operations.length > 0">
                    ·
                    <span class="revenu">
                        {{ formatMontant(totaux.revenus) }} de revenus
                    </span>
                    ·
                    <span class="depense">
                        {{ formatMontant(totaux.depenses) }} de dépenses
                    </span>
                </template>
            </p>

            <section class="carte liste" :class="{ rechargement: isLoading }">
                <p v-if="operations.length === 0 && !isLoading" class="vide">
                    {{
                        filtresActifs
                            ? "Aucune opération ne correspond à ces filtres."
                            : "Aucune opération sur la période. Appuyez sur N ou sur « + » pour en saisir une."
                    }}
                </p>

                <div v-for="jour in parJour" :key="jour.date" class="jour">
                    <h2>{{ formatDateLongue(jour.date) }}</h2>
                    <ul>
                        <li v-for="op in jour.operations" :key="op.id">
                            <span class="icone">{{
                                icone(op.categorieId)
                            }}</span>
                            <div class="texte">
                                <span class="titre">
                                    {{ titre(op) }}
                                    <RouterLink
                                        v-if="op.recurrenceId !== null"
                                        :to="{
                                            name: 'recurrences',
                                            query: {
                                                recurrence: op.recurrenceId,
                                            },
                                        }"
                                        class="badge"
                                        title="Générée par une récurrence (voir la récurrence)"
                                        >🔁</RouterLink
                                    >
                                </span>
                                <span v-if="details(op)" class="details">
                                    {{ details(op) }}
                                </span>
                            </div>
                            <span
                                class="montant"
                                :class="classeMontant(op.type)"
                            >
                                {{ formatMontantSigne(op.montant, op.type) }}
                            </span>

                            <div
                                v-if="aSupprimer === op.id"
                                class="actions confirmation"
                            >
                                <span>Supprimer ?</span>
                                <button
                                    type="button"
                                    class="bouton-danger"
                                    :disabled="enCours === op.id"
                                    @click="supprimer(op)"
                                >
                                    Oui
                                </button>
                                <button
                                    type="button"
                                    class="bouton-action"
                                    @click="aSupprimer = null"
                                >
                                    Non
                                </button>
                            </div>
                            <div v-else class="actions">
                                <button
                                    type="button"
                                    class="bouton-action"
                                    title="Modifier"
                                    aria-label="Modifier"
                                    :disabled="enCours === op.id"
                                    @click="modifier(op)"
                                >
                                    ✏️
                                </button>
                                <button
                                    v-if="op.recurrenceId === null"
                                    type="button"
                                    class="bouton-action"
                                    title="Répéter chaque mois"
                                    aria-label="Répéter chaque mois"
                                    :disabled="enCours === op.id"
                                    @click="repeter(op)"
                                >
                                    🔁
                                </button>
                                <button
                                    type="button"
                                    class="bouton-action"
                                    title="Supprimer"
                                    aria-label="Supprimer"
                                    :disabled="enCours === op.id"
                                    @click="aSupprimer = op.id"
                                >
                                    🗑️
                                </button>
                            </div>
                        </li>
                    </ul>
                </div>
            </section>
        </template>
    </main>
</template>

<style scoped>
.operations {
    color: #45335c;
    max-width: 900px;
    margin: 0 auto;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

h1 {
    margin: 0;
    font-size: 1.6rem;
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

.rechargement {
    opacity: 0.5;
    transition: opacity 0.2s;
}

/* Période */
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
    flex-shrink: 0;
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

/* Filtres */
.filtres {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.6rem;
    padding: 0.9rem;
}

.filtres input,
.filtres select {
    padding: 0.5rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #000000;
    font-size: 0.95rem;
    font-family: inherit;
    min-width: 0;
}

.recherche {
    flex: 1 1 220px;
}

.filtres select {
    flex: 0 1 200px;
}

.bouton-lien {
    border: none;
    background: none;
    color: #45335c;
    text-decoration: underline;
    cursor: pointer;
    font-size: 0.9rem;
}

.resume {
    margin: 0;
    color: #81758e;
}

/* Liste */
.liste {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.jour h2 {
    margin: 0 0 0.25rem;
    font-size: 0.85rem;
    font-weight: normal;
    color: #81758e;
    text-transform: capitalize;
}

ul {
    list-style: none;
    margin: 0;
    padding: 0;
}

li {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0;
    border-bottom: 1px solid #f1e9f8;
}

li:last-child {
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
    text-decoration: none;
}

.details {
    font-size: 0.85rem;
    color: #81758e;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.montant {
    font-weight: bold;
    white-space: nowrap;
}

.actions {
    display: flex;
    align-items: center;
    gap: 0.25rem;
}

.confirmation span {
    font-size: 0.85rem;
    margin-right: 0.25rem;
}

.bouton-action,
.bouton-danger {
    padding: 0.3rem 0.5rem;
    border: 1px solid transparent;
    border-radius: 8px;
    background: none;
    cursor: pointer;
    font-size: 0.9rem;
}

.bouton-action:hover {
    border-color: #e3d3f1;
    background: #f7f2fb;
}

.bouton-danger {
    background: #c62727;
    color: #ffffff;
}

.bouton-action:disabled,
.bouton-danger:disabled {
    opacity: 0.5;
    cursor: default;
}
</style>
