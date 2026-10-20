<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { comptesService } from "@/services/comptesService.ts";
import { useReferentiels } from "@/composables/useReferentiels.ts";
import { useNotificationStore } from "@/stores/notification";
import { CircleStop, Pencil, Plus, Repeat } from "@lucide/vue";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import LigneOperationComponent from "@/components/LigneOperationComponent.vue";
import RecurrenceFormComponent from "@/components/RecurrenceFormComponent.vue";
import type { Libelle, Recurrence, TypeOperation } from "@/types/comptes.ts";
import { formatDateLongue, formatMontant } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import { idDepuisQuery } from "@/utils/routage.ts";
import { decrireCalendrier } from "@/utils/recurrences.ts";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();

const { libelles, chargerReferentiels, titre, categorie, details } =
    useReferentiels();

const recurrences = ref<Recurrence[]>([]);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

async function charger() {
    errorMessage.value = null;
    try {
        const [liste] = await Promise.all([
            comptesService.getRecurrences(),
            chargerReferentiels(),
        ]);
        recurrences.value = liste;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos récurrences."
        );
    } finally {
        isLoading.value = false;
    }
}

onMounted(async () => {
    await charger();
    // Lien depuis une opération générée (badge « Récurrente ») : ouvre directement sa récurrence
    const id = idDepuisQuery(route.query.recurrence);
    if (id !== undefined) {
        const r = recurrences.value.find((x) => x.id === id);
        if (r) modifier(r);
        await router.replace({ query: {} });
    }
});

const actives = computed(() => recurrences.value.filter((r) => r.active));
const arretees = computed(() => recurrences.value.filter((r) => !r.active));

// Ce qui part et rentre chaque mois avec les récurrences mensuelles en cours
function totalMensuel(type: TypeOperation): number {
    const total = actives.value
        .filter((r) => r.frequence === "MENSUELLE" && r.type === type)
        .reduce((somme, r) => somme + r.montant, 0);
    return Math.round(total * 100) / 100;
}
const depensesMensuelles = computed(() => totalMensuel("DEPENSE"));
const revenusMensuels = computed(() => totalMensuel("REVENU"));

// « Le 5 de chaque mois · Logement · Compte courant »
function meta(r: Recurrence): string {
    const calendrier = decrireCalendrier(r.frequence, r.jour, r.dateDebut);
    const suite = details(r, true);
    return suite ? `${calendrier} · ${suite}` : calendrier;
}

// ── Formulaire ──

const formulaireOuvert = ref(false);
const aModifier = ref<Recurrence | null>(null);
const libelleAModifier = ref<Libelle | null>(null);

function nouvelle() {
    aModifier.value = null;
    libelleAModifier.value = null;
    formulaireOuvert.value = true;
}

function modifier(r: Recurrence) {
    aModifier.value = r;
    libelleAModifier.value =
        r.libelleId !== null ? (libelles.value.get(r.libelleId) ?? null) : null;
    formulaireOuvert.value = true;
}

async function surEnregistrement(r: Recurrence, creation: boolean) {
    formulaireOuvert.value = false;
    notification.success(
        creation
            ? `Récurrence créée. Prochaine opération : ${formatDateLongue(r.prochaineOccurrence)}.`
            : "Récurrence modifiée."
    );
    // Un libellé a pu être créé dans le formulaire
    await charger();
}

// ── Arrêt, en deux temps, sans boîte de dialogue ──

const aArreter = ref<number | null>(null);
const enCours = ref<number | null>(null);

async function arreter(r: Recurrence) {
    enCours.value = r.id;
    try {
        const arretee = await comptesService.arreterRecurrence(r.id);
        notification.success(
            arretee
                ? `« ${titre(r)} » est arrêtée : plus aucune opération après aujourd'hui. Les opérations déjà créées restent.`
                : `« ${titre(r)} » est supprimée (elle n'avait encore créé aucune opération).`
        );
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible d'arrêter la récurrence.")
        );
    } finally {
        enCours.value = null;
        aArreter.value = null;
    }
}
</script>

<template>
    <EnTeteComponent />

    <main class="page page-etroite recurrences">
        <div class="en-tete-page">
            <div>
                <h1 class="titre-page">Récurrences</h1>
                <p class="sous-titre">
                    Loyer, salaire, abonnements : l'application les ajoute toute
                    seule au bon jour.
                </p>
            </div>
            <!-- Secondaire : le seul bouton doré de l'écran reste « Nouvelle opération » -->
            <button type="button" class="btn btn-secondaire" @click="nouvelle">
                <Plus aria-hidden="true" />
                Nouvelle récurrence
            </button>
        </div>

        <p v-if="isLoading" class="message">Chargement...</p>
        <p v-else-if="errorMessage" class="message erreur" role="alert">
            {{ errorMessage }}
        </p>

        <template v-else>
            <section v-if="recurrences.length === 0" class="carte">
                <p class="vide">
                    <Repeat aria-hidden="true" />
                    <span>
                        Aucune récurrence pour l'instant. Ajoutez une fois ce
                        qui revient chaque mois ou chaque année ; vous pouvez
                        aussi transformer une opération avec le bouton « Répéter
                        chaque mois » de l'écran Opérations.
                    </span>
                    <button
                        type="button"
                        class="btn btn-discret"
                        @click="nouvelle"
                    >
                        Ajouter une récurrence
                    </button>
                </p>
            </section>

            <template v-else>
                <div v-if="actives.length > 0" class="totaux">
                    <div class="carte total">
                        <span class="intitule">Dépenses chaque mois</span>
                        <span class="valeur num negatif">
                            {{ formatMontant(depensesMensuelles) }}
                        </span>
                    </div>
                    <div class="carte total">
                        <span class="intitule">Revenus chaque mois</span>
                        <span class="valeur num positif">
                            {{ formatMontant(revenusMensuels) }}
                        </span>
                    </div>
                </div>

                <section class="carte">
                    <h2 class="titre-carte">En cours</h2>
                    <p v-if="actives.length === 0" class="vide">
                        <Repeat aria-hidden="true" />
                        Aucune récurrence en cours.
                    </p>
                    <ul v-else class="nue">
                        <LigneOperationComponent
                            v-for="r in actives"
                            :key="r.id"
                            :categorie="categorie(r.categorieId)"
                            :titre="titre(r)"
                            :meta="meta(r)"
                            :montant="r.montant"
                            :type="r.type"
                        >
                            <template #meta>
                                <span class="meta">
                                    Prochaine :
                                    {{
                                        formatDateLongue(r.prochaineOccurrence)
                                    }}
                                    <template v-if="r.dateFin">
                                        · jusqu'au
                                        {{ formatDateLongue(r.dateFin) }}
                                    </template>
                                </span>
                            </template>

                            <template #actions>
                                <div
                                    v-if="aArreter === r.id"
                                    class="actions confirmation"
                                >
                                    <span>Arrêter ?</span>
                                    <button
                                        type="button"
                                        class="btn btn-danger"
                                        :disabled="enCours === r.id"
                                        @click="arreter(r)"
                                    >
                                        Arrêter
                                    </button>
                                    <button
                                        type="button"
                                        class="btn btn-discret"
                                        @click="aArreter = null"
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
                                        :disabled="enCours === r.id"
                                        @click="modifier(r)"
                                    >
                                        <Pencil aria-hidden="true" />
                                    </button>
                                    <button
                                        type="button"
                                        class="btn btn-icone"
                                        title="Arrêter"
                                        aria-label="Arrêter"
                                        :disabled="enCours === r.id"
                                        @click="aArreter = r.id"
                                    >
                                        <CircleStop aria-hidden="true" />
                                    </button>
                                </div>
                            </template>
                        </LigneOperationComponent>
                    </ul>
                </section>

                <section v-if="arretees.length > 0" class="carte">
                    <h2 class="titre-carte">Arrêtées</h2>
                    <ul class="nue">
                        <LigneOperationComponent
                            v-for="r in arretees"
                            :key="r.id"
                            :categorie="categorie(r.categorieId)"
                            :titre="titre(r)"
                            :meta="meta(r)"
                            :montant="r.montant"
                            :type="r.type"
                            estompee
                        >
                            <template v-if="r.dateFin" #meta>
                                <span class="meta">
                                    Terminée le
                                    {{ formatDateLongue(r.dateFin) }}
                                </span>
                            </template>
                            <template #actions>
                                <div class="actions">
                                    <button
                                        type="button"
                                        class="btn btn-icone"
                                        title="Modifier ou reprendre"
                                        aria-label="Modifier ou reprendre"
                                        @click="modifier(r)"
                                    >
                                        <Pencil aria-hidden="true" />
                                    </button>
                                </div>
                            </template>
                        </LigneOperationComponent>
                    </ul>
                </section>
            </template>
        </template>
    </main>

    <RecurrenceFormComponent
        v-if="formulaireOuvert"
        :recurrence="aModifier"
        :libelle="libelleAModifier"
        @fermer="formulaireOuvert = false"
        @enregistree="surEnregistrement"
    />
</template>

<style scoped>
.totaux {
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

.titre-carte {
    margin-bottom: 8px;
}

/* Largeur de deux boutons : les montants restent alignés, même pour les récurrences arrêtées */
.actions {
    flex-wrap: nowrap;
    justify-content: flex-end;
    min-width: 84px;
}

/* Sur téléphone, les boutons passent sous le montant, alignés à droite */
@media (max-width: 640px) {
    .actions {
        flex-basis: 100%;
        margin-top: -4px;
    }
}
</style>
