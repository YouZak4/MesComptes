<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { comptesService } from "@/services/comptesService.ts";
import { useReferentiels } from "@/composables/useReferentiels.ts";
import { useNotificationStore } from "@/stores/notification";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import RecurrenceFormComponent from "@/components/RecurrenceFormComponent.vue";
import type { Libelle, Recurrence, TypeOperation } from "@/types/comptes.ts";
import {
    formatDateLongue,
    formatMontant,
    formatMontantSigne,
} from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import { idDepuisQuery } from "@/utils/routage.ts";
import { decrireCalendrier } from "@/utils/recurrences.ts";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();

const { libelles, chargerReferentiels, titre, icone, details } =
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
    // Lien depuis une opération générée (badge 🔁) : ouvre directement sa récurrence
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

function calendrier(r: Recurrence): string {
    return decrireCalendrier(r.frequence, r.jour, r.dateDebut);
}

function classeMontant(type: TypeOperation): string {
    return type === "DEPENSE" ? "depense" : "revenu";
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

    <main class="recurrences">
        <div class="titre-page">
            <div>
                <h1>Récurrences</h1>
                <p class="sous-titre">
                    Loyer, salaire, abonnements : l'application les ajoute toute
                    seule au bon jour.
                </p>
            </div>
            <button type="button" class="bouton-principal" @click="nouvelle">
                Nouvelle récurrence
            </button>
        </div>

        <p v-if="isLoading" class="message">Chargement...</p>
        <p v-else-if="errorMessage" class="message erreur">
            {{ errorMessage }}
        </p>

        <template v-else>
            <section v-if="recurrences.length === 0" class="carte">
                <p class="vide">
                    Aucune récurrence pour l'instant. Ajoutez une fois ce qui
                    revient chaque mois ou chaque année ; vous pouvez aussi
                    transformer une opération avec le bouton 🔁 de l'écran
                    Opérations.
                </p>
            </section>

            <template v-else>
                <p v-if="actives.length > 0" class="resume">
                    Chaque mois :
                    <span class="depense"
                        >{{ formatMontant(depensesMensuelles) }} de
                        dépenses</span
                    >
                    ·
                    <span class="revenu"
                        >{{ formatMontant(revenusMensuels) }} de revenus</span
                    >
                </p>

                <section class="carte">
                    <h2>En cours</h2>
                    <p v-if="actives.length === 0" class="vide">
                        Aucune récurrence en cours.
                    </p>
                    <ul v-else>
                        <li v-for="r in actives" :key="r.id">
                            <span class="icone">{{
                                icone(r.categorieId)
                            }}</span>
                            <div class="texte">
                                <span class="titre">{{ titre(r) }}</span>
                                <span class="details">
                                    {{ calendrier(r) }}
                                    <template v-if="details(r, true)">
                                        · {{ details(r, true) }}
                                    </template>
                                </span>
                                <span class="echeance">
                                    Prochaine :
                                    {{
                                        formatDateLongue(r.prochaineOccurrence)
                                    }}
                                    <template v-if="r.dateFin">
                                        · jusqu'au
                                        {{ formatDateLongue(r.dateFin) }}
                                    </template>
                                </span>
                            </div>
                            <span
                                class="montant"
                                :class="classeMontant(r.type)"
                            >
                                {{ formatMontantSigne(r.montant, r.type) }}
                            </span>

                            <div
                                v-if="aArreter === r.id"
                                class="actions confirmation"
                            >
                                <span>Arrêter ?</span>
                                <button
                                    type="button"
                                    class="bouton-danger"
                                    :disabled="enCours === r.id"
                                    @click="arreter(r)"
                                >
                                    Oui
                                </button>
                                <button
                                    type="button"
                                    class="bouton-action"
                                    @click="aArreter = null"
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
                                    :disabled="enCours === r.id"
                                    @click="modifier(r)"
                                >
                                    ✏️
                                </button>
                                <button
                                    type="button"
                                    class="bouton-action"
                                    title="Arrêter"
                                    aria-label="Arrêter"
                                    :disabled="enCours === r.id"
                                    @click="aArreter = r.id"
                                >
                                    ⏹️
                                </button>
                            </div>
                        </li>
                    </ul>
                </section>

                <section v-if="arretees.length > 0" class="carte arretees">
                    <h2>Arrêtées</h2>
                    <ul>
                        <li v-for="r in arretees" :key="r.id">
                            <span class="icone">{{
                                icone(r.categorieId)
                            }}</span>
                            <div class="texte">
                                <span class="titre">{{ titre(r) }}</span>
                                <span class="details">
                                    {{ calendrier(r) }}
                                    <template v-if="details(r, true)">
                                        · {{ details(r, true) }}
                                    </template>
                                </span>
                                <span v-if="r.dateFin" class="echeance">
                                    Terminée le
                                    {{ formatDateLongue(r.dateFin) }}
                                </span>
                            </div>
                            <span
                                class="montant"
                                :class="classeMontant(r.type)"
                            >
                                {{ formatMontantSigne(r.montant, r.type) }}
                            </span>
                            <div class="actions">
                                <button
                                    type="button"
                                    class="bouton-action"
                                    title="Modifier ou reprendre"
                                    aria-label="Modifier ou reprendre"
                                    @click="modifier(r)"
                                >
                                    ✏️
                                </button>
                            </div>
                        </li>
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
.recurrences {
    color: #45335c;
    max-width: 900px;
    margin: 0 auto;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.titre-page {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
}

h1 {
    margin: 0;
    font-size: 1.6rem;
}

.sous-titre {
    margin: 0.25rem 0 0;
    color: #81758e;
}

h2 {
    margin: 0 0 0.5rem;
    font-size: 1.1rem;
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

.arretees .titre,
.arretees .montant,
.arretees .icone {
    opacity: 0.6;
}

.vide {
    color: #81758e;
    margin: 0;
}

.resume {
    margin: 0;
    color: #81758e;
}

.depense {
    color: #c62727;
}

.revenu {
    color: #14a81a;
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

.details,
.echeance {
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

.bouton-principal {
    padding: 0.6rem 1rem;
    border: none;
    border-radius: 10px;
    background: #45335c;
    color: #ffffff;
    font-size: 0.95rem;
    cursor: pointer;
}

.bouton-principal:hover {
    background: #5a4377;
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
