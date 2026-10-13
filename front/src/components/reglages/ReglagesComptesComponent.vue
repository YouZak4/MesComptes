<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { vFocus } from "@/directives/focus.ts";
import { comptesService } from "@/services/comptesService.ts";
import { useNotificationStore } from "@/stores/notification";
import type { Compte } from "@/types/comptes.ts";
import { formatDateLongue, formatMontant, versIso } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";

const notification = useNotificationStore();

const comptes = ref<Compte[]>([]);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

async function charger() {
    try {
        comptes.value = await comptesService.getComptes();
        errorMessage.value = null;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos comptes."
        );
    } finally {
        isLoading.value = false;
    }
}

onMounted(charger);

// Comptes en cours d'abord, archivés ensuite
const tries = computed(() =>
    [...comptes.value].sort((a, b) => Number(a.archive) - Number(b.archive))
);

// Solde initial : peut être négatif (découvert), deux décimales au plus
function lireSolde(texte: string): number | null {
    const nettoye = texte
        .replace(/\s/g, "")
        .replace(",", ".")
        .replace("−", "-");
    if (!/^-?\d+(\.\d{1,2})?$/.test(nettoye)) return null;
    return Number(nettoye);
}

function versTexte(montant: number): string {
    return String(montant).replace(".", ",");
}

// ── Formulaire (ajout, ou modification d'un compte) ──

const enEdition = ref<number | "nouveau" | null>(null);
const nom = ref("");
const soldeTexte = ref("");
const dateSolde = ref(versIso(new Date()));
const erreurFormulaire = ref<string | null>(null);
const enregistrementEnCours = ref(false);

function ouvrirAjout() {
    enEdition.value = "nouveau";
    nom.value = "";
    soldeTexte.value = "0";
    dateSolde.value = versIso(new Date());
    erreurFormulaire.value = null;
}

function ouvrirModification(compte: Compte) {
    enEdition.value = compte.id;
    nom.value = compte.nom;
    soldeTexte.value = versTexte(compte.soldeInitial);
    dateSolde.value = compte.dateSoldeInitial;
    erreurFormulaire.value = null;
}

function annuler() {
    enEdition.value = null;
}

async function enregistrer() {
    if (enregistrementEnCours.value) return;
    const soldeInitial = lireSolde(soldeTexte.value);
    if (soldeInitial === null) {
        erreurFormulaire.value =
            "Le solde initial doit être un nombre, avec au plus deux décimales.";
        return;
    }
    enregistrementEnCours.value = true;
    erreurFormulaire.value = null;
    try {
        const valeurs = {
            nom: nom.value.trim(),
            soldeInitial,
            dateSoldeInitial: dateSolde.value,
            archive: false,
        };
        if (enEdition.value === "nouveau") {
            await comptesService.creerCompte(valeurs);
            notification.success(`Compte « ${valeurs.nom} » créé.`);
        } else if (enEdition.value !== null) {
            const actuel = comptes.value.find((c) => c.id === enEdition.value);
            await comptesService.modifierCompte(enEdition.value, {
                ...valeurs,
                archive: actuel?.archive ?? false,
            });
            notification.success("Compte modifié.");
        }
        enEdition.value = null;
        await charger();
    } catch (e: any) {
        erreurFormulaire.value = messageErreur(
            e,
            "Impossible d'enregistrer le compte."
        );
    } finally {
        enregistrementEnCours.value = false;
    }
}

// ── Retrait (archivage ou suppression) et désarchivage ──

const aRetirer = ref<number | null>(null);
const enCours = ref<number | null>(null);

async function retirer(compte: Compte) {
    enCours.value = compte.id;
    try {
        const archive = await comptesService.supprimerCompte(compte.id);
        notification.success(
            archive
                ? `« ${compte.nom} » est archivé : il n'est plus proposé à la saisie, son historique reste.`
                : `« ${compte.nom} » est supprimé (il n'avait aucune opération).`
        );
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de retirer le compte.")
        );
    } finally {
        enCours.value = null;
        aRetirer.value = null;
    }
}

async function desarchiver(compte: Compte) {
    enCours.value = compte.id;
    try {
        await comptesService.modifierCompte(compte.id, {
            nom: compte.nom,
            soldeInitial: compte.soldeInitial,
            dateSoldeInitial: compte.dateSoldeInitial,
            archive: false,
        });
        notification.success(`« ${compte.nom} » est de nouveau proposé.`);
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de désarchiver le compte.")
        );
    } finally {
        enCours.value = null;
    }
}
</script>

<template>
    <p v-if="isLoading" class="message">Chargement...</p>
    <p v-else-if="errorMessage" class="message erreur">{{ errorMessage }}</p>

    <section v-else class="carte">
        <h2>Comptes</h2>
        <p class="aide">
            Le solde d'un compte part de son solde initial, au matin de sa date
            : les opérations de ce jour-là comptent.
        </p>

        <ul class="liste">
            <li
                v-for="compte in tries"
                :key="compte.id"
                :class="{ inactif: compte.archive }"
            >
                <form
                    v-if="enEdition === compte.id"
                    class="formulaire edition"
                    @submit.prevent="enregistrer"
                >
                    <label class="champ large">
                        Nom
                        <input
                            v-focus
                            v-model="nom"
                            type="text"
                            maxlength="100"
                            required
                        />
                    </label>
                    <label class="champ">
                        Solde initial (€)
                        <input
                            v-model="soldeTexte"
                            type="text"
                            inputmode="decimal"
                            required
                        />
                    </label>
                    <label class="champ">
                        Au
                        <input v-model="dateSolde" type="date" required />
                    </label>
                    <div class="actions">
                        <button
                            type="submit"
                            class="bouton-principal"
                            :disabled="enregistrementEnCours"
                        >
                            Enregistrer
                        </button>
                        <button
                            type="button"
                            class="bouton-secondaire"
                            @click="annuler"
                        >
                            Annuler
                        </button>
                    </div>
                    <p v-if="erreurFormulaire" class="erreur" role="alert">
                        {{ erreurFormulaire }}
                    </p>
                </form>

                <template v-else>
                    <div class="texte">
                        <span class="titre">
                            {{ compte.nom }}
                            <span v-if="compte.archive" class="badge"
                                >Archivé</span
                            >
                        </span>
                        <span class="details">
                            Solde initial
                            {{ formatMontant(compte.soldeInitial) }} au
                            {{ formatDateLongue(compte.dateSoldeInitial) }}
                        </span>
                    </div>
                    <span
                        class="solde"
                        :class="compte.solde < 0 ? 'depense' : 'revenu'"
                    >
                        {{ formatMontant(compte.solde) }}
                    </span>

                    <div
                        v-if="aRetirer === compte.id"
                        class="actions confirmation"
                    >
                        <span>Retirer ce compte ?</span>
                        <button
                            type="button"
                            class="bouton-danger"
                            :disabled="enCours === compte.id"
                            @click="retirer(compte)"
                        >
                            Oui
                        </button>
                        <button
                            type="button"
                            class="bouton-action"
                            @click="aRetirer = null"
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
                            :disabled="enCours === compte.id"
                            @click="ouvrirModification(compte)"
                        >
                            ✏️
                        </button>
                        <button
                            v-if="compte.archive"
                            type="button"
                            class="bouton-action"
                            :disabled="enCours === compte.id"
                            @click="desarchiver(compte)"
                        >
                            Désarchiver
                        </button>
                        <button
                            v-else
                            type="button"
                            class="bouton-action"
                            title="Archiver (ou supprimer s'il n'a aucune opération)"
                            aria-label="Retirer"
                            :disabled="enCours === compte.id"
                            @click="aRetirer = compte.id"
                        >
                            🗄️
                        </button>
                    </div>
                </template>
            </li>
        </ul>

        <form
            v-if="enEdition === 'nouveau'"
            class="formulaire ajout"
            @submit.prevent="enregistrer"
        >
            <label class="champ large">
                Nom
                <input
                    v-focus
                    v-model="nom"
                    type="text"
                    maxlength="100"
                    placeholder="Livret A, Compte joint…"
                    required
                />
            </label>
            <label class="champ">
                Solde initial (€)
                <input
                    v-model="soldeTexte"
                    type="text"
                    inputmode="decimal"
                    required
                />
            </label>
            <label class="champ">
                Au
                <input v-model="dateSolde" type="date" required />
            </label>
            <div class="actions">
                <button
                    type="submit"
                    class="bouton-principal"
                    :disabled="enregistrementEnCours"
                >
                    Ajouter
                </button>
                <button
                    type="button"
                    class="bouton-secondaire"
                    @click="annuler"
                >
                    Annuler
                </button>
            </div>
            <p v-if="erreurFormulaire" class="erreur" role="alert">
                {{ erreurFormulaire }}
            </p>
        </form>
        <button
            v-else
            type="button"
            class="bouton-secondaire ajouter"
            @click="ouvrirAjout"
        >
            + Ajouter un compte
        </button>
    </section>
</template>

<style scoped>
.liste {
    margin: 0.75rem 0 !important;
}

.edition,
.ajout {
    flex: 1;
}

.ajout {
    padding-top: 0.75rem;
    border-top: 1px solid #f1e9f8;
}

.erreur {
    flex-basis: 100%;
}

.solde {
    font-weight: bold;
    white-space: nowrap;
}
</style>
