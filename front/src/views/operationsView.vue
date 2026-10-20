<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { comptesService } from "@/services/comptesService.ts";
import { useReferentiels } from "@/composables/useReferentiels.ts";
import { useNotificationStore } from "@/stores/notification";
import { useSaisieStore } from "@/stores/saisie";
import { Pencil, Receipt, Repeat, SearchX, Trash } from "@lucide/vue";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import LigneOperationComponent from "@/components/LigneOperationComponent.vue";
import SelecteurPeriodeComponent from "@/components/SelecteurPeriodeComponent.vue";
import type { Operation, Periode } from "@/types/comptes.ts";
import { formatDateLongue, formatMontant, versIso } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import { idConnu, idDepuisQuery } from "@/utils/routage.ts";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();
const saisie = useSaisieStore();

const {
    comptes,
    categories,
    libelles,
    nomsComptes,
    chargerReferentiels,
    titre,
    categorie,
    details: detailsOperation,
} = useReferentiels();

const periodes = ref<Periode[]>([]);
const operations = ref<Operation[]>([]);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

// Filtres dans l'URL : un rechargement ou un lien les garde ; un id inconnu est ignoré
const compteId = computed(() => idConnu(route.query.compte, nomsComptes.value));
const categorieId = computed(() =>
    idConnu(route.query.categorie, categories.value)
);
const libelleId = computed(() => idConnu(route.query.libelle, libelles.value));
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

// Sans période dans l'URL, c'est celle d'aujourd'hui qui est choisie
function allerALaPeriodeCourante() {
    changerFiltre("periode", "");
}

const periodeEnCours = computed(() => {
    if (!periode.value) return true;
    const aujourdhui = versIso(new Date());
    return (
        periode.value.dateDebut <= aujourdhui &&
        aujourdhui <= periode.value.dateFin
    );
});

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

// Augmente à chaque chargement complet : le watch des filtres recharge alors la liste,
// une seule fois même si la période ou les filtres changent en même temps
const nbChargements = ref(0);

async function toutCharger() {
    isLoading.value = true;
    try {
        const [listePeriodes] = await Promise.all([
            comptesService.getPeriodes(),
            chargerReferentiels(),
        ]);
        periodes.value = listePeriodes;
        nbChargements.value++;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos comptes."
        );
        isLoading.value = false;
    }
}

onMounted(toutCharger);

// Sur l'id de la période : recharger la liste des périodes ne doit pas relancer la recherche
watch(
    [
        () => periode.value?.id,
        compteId,
        categorieId,
        libelleId,
        recherche,
        nbChargements,
    ],
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

    <main class="page page-etroite operations">
        <p v-if="isLoading && periodes.length === 0" class="message">
            Chargement...
        </p>

        <p
            v-else-if="errorMessage && periodes.length === 0"
            class="message erreur"
            role="alert"
        >
            {{ errorMessage }}
        </p>

        <section v-else-if="!periode" class="carte vide">
            Aucune période n'est encore définie.
            <RouterLink :to="{ name: 'accueil' }">
                Créez-la depuis le tableau de bord.
            </RouterLink>
        </section>

        <template v-else>
            <div class="en-tete-page">
                <div>
                    <h1 class="titre-page">Opérations · {{ periode.nom }}</h1>
                    <p class="sous-titre">
                        du {{ formatDateLongue(periode.dateDebut) }} au
                        {{ formatDateLongue(periode.dateFin) }}
                    </p>
                </div>
                <SelecteurPeriodeComponent
                    :precedente="periodePrecedente !== null"
                    :suivante="periodeSuivante !== null"
                    :en-cours="periodeEnCours"
                    @precedente="allerALaPeriode(periodePrecedente)"
                    @suivante="allerALaPeriode(periodeSuivante)"
                    @courante="allerALaPeriodeCourante"
                />
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
                        {{ c.nom }}
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
                    class="btn btn-discret"
                    @click="effacerFiltres"
                >
                    Effacer les filtres
                </button>
            </div>

            <p v-if="errorMessage" class="message erreur" role="alert">
                {{ errorMessage }}
            </p>

            <!-- Résumé de ce qui est affiché -->
            <p class="resume">
                {{ operations.length }}
                {{ operations.length > 1 ? "opérations" : "opération" }}
                <template v-if="operations.length > 0">
                    ·
                    <span class="num positif">
                        {{ formatMontant(totaux.revenus) }} de revenus
                    </span>
                    ·
                    <span class="num negatif">
                        {{ formatMontant(totaux.depenses) }} de dépenses
                    </span>
                </template>
            </p>

            <section class="carte liste" :class="{ rechargement: isLoading }">
                <p v-if="operations.length === 0 && !isLoading" class="vide">
                    <template v-if="filtresActifs">
                        <SearchX aria-hidden="true" />
                        Aucune opération ne correspond à ces filtres.
                    </template>
                    <template v-else>
                        <Receipt aria-hidden="true" />
                        Aucune opération sur la période. Appuyez sur N ou sur «
                        Nouvelle opération » pour en saisir une.
                    </template>
                </p>

                <div v-for="jour in parJour" :key="jour.date" class="jour">
                    <h2 class="date-jour">
                        {{ formatDateLongue(jour.date) }}
                    </h2>
                    <ul class="nue">
                        <LigneOperationComponent
                            v-for="op in jour.operations"
                            :key="op.id"
                            :categorie="categorie(op.categorieId)"
                            :titre="titre(op)"
                            :meta="details(op)"
                            :montant="op.montant"
                            :type="op.type"
                        >
                            <template v-if="op.recurrenceId !== null" #badges>
                                <RouterLink
                                    :to="{
                                        name: 'recurrences',
                                        query: { recurrence: op.recurrenceId },
                                    }"
                                    class="badge"
                                    title="Générée par une récurrence (voir la récurrence)"
                                >
                                    <Repeat aria-hidden="true" />
                                    Récurrente
                                </RouterLink>
                            </template>

                            <template #actions>
                                <div
                                    v-if="aSupprimer === op.id"
                                    class="actions confirmation"
                                >
                                    <span>Supprimer ?</span>
                                    <button
                                        type="button"
                                        class="btn btn-danger"
                                        :disabled="enCours === op.id"
                                        @click="supprimer(op)"
                                    >
                                        Supprimer
                                    </button>
                                    <button
                                        type="button"
                                        class="btn btn-discret"
                                        @click="aSupprimer = null"
                                    >
                                        Annuler
                                    </button>
                                </div>
                                <div v-else class="actions">
                                    <button
                                        type="button"
                                        class="btn btn-icone"
                                        title="Modifier"
                                        aria-label="Modifier"
                                        :disabled="enCours === op.id"
                                        @click="modifier(op)"
                                    >
                                        <Pencil aria-hidden="true" />
                                    </button>
                                    <button
                                        v-if="op.recurrenceId === null"
                                        type="button"
                                        class="btn btn-icone"
                                        title="Répéter chaque mois"
                                        aria-label="Répéter chaque mois"
                                        :disabled="enCours === op.id"
                                        @click="repeter(op)"
                                    >
                                        <Repeat aria-hidden="true" />
                                    </button>
                                    <button
                                        type="button"
                                        class="btn btn-icone"
                                        title="Supprimer"
                                        aria-label="Supprimer"
                                        :disabled="enCours === op.id"
                                        @click="aSupprimer = op.id"
                                    >
                                        <Trash aria-hidden="true" />
                                    </button>
                                </div>
                            </template>
                        </LigneOperationComponent>
                    </ul>
                </div>
            </section>
        </template>
    </main>
</template>

<style scoped>
/* Filtres */
.filtres {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    padding: 14px;
}

.recherche {
    flex: 1 1 220px;
}

.filtres select {
    flex: 0 1 200px;
}

.resume {
    color: var(--encre-douce);
    font-weight: 600;
}

/* Liste */
.liste {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.date-jour {
    margin-bottom: 2px;
    font-size: 12px;
    font-weight: 700;
    color: var(--encre-douce);
}

/* Largeur de trois boutons : les montants restent alignés en colonne, même sans « Répéter » */
.actions {
    flex-wrap: nowrap;
    justify-content: flex-end;
    min-width: 128px;
}

@media (max-width: 640px) {
    .filtres select {
        flex: 1 1 100%;
    }

    /* Sur téléphone, les boutons passent sous le montant, alignés à droite */
    .actions {
        flex-basis: 100%;
        margin-top: -4px;
    }
}
</style>
