<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { CircleAlert, Plus } from "@lucide/vue";
import { comptesService } from "@/services/comptesService.ts";
import ChampLibelleComponent from "@/components/ChampLibelleComponent.vue";
import { useNotificationStore } from "@/stores/notification";
import { type OperationAModifier, useSaisieStore } from "@/stores/saisie";
import type { Categorie, Compte, Libelle } from "@/types/comptes.ts";
import { versIso } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";

const saisie = useSaisieStore();
const notification = useNotificationStore();

const comptes = ref<Compte[]>([]);
const categories = ref<Categorie[]>([]);
const chargementReferentiels = ref(false);
const enregistrementEnCours = ref(false);
const erreur = ref<string | null>(null);

const montantTexte = ref("");
const libelleTexte = ref("");
// Libellé existant désigné par le texte tapé, null pour un nouveau libellé
const libelleExistant = ref<Libelle | null>(null);
const categorieId = ref<number | null>(null);
const date = ref(versIso(new Date()));
const compteId = ref<number | null>(null);
const note = ref("");

const champMontant = ref<HTMLInputElement | null>(null);

// ── Ouverture : touche N ou bouton « Nouvelle opération » ──

function estUnChampDeSaisie(cible: EventTarget | null): boolean {
    if (!(cible instanceof HTMLElement)) return false;
    return (
        cible.isContentEditable ||
        ["INPUT", "TEXTAREA", "SELECT"].includes(cible.tagName)
    );
}

function surTouche(event: KeyboardEvent) {
    if (saisie.ouverte) {
        // Échap ferme la fenêtre (dans le libellé, il ferme d'abord les suggestions)
        if (event.key === "Escape") fermer();
        return;
    }
    if (event.repeat) return;
    if (event.ctrlKey || event.metaKey || event.altKey) return;
    if (event.key !== "n" && event.key !== "N") return;
    // Une autre fenêtre est déjà ouverte (formulaire de récurrence…)
    if (document.querySelector('[aria-modal="true"]')) return;
    // Taper un « n » dans un champ ne doit pas ouvrir la saisie
    if (estUnChampDeSaisie(event.target)) return;
    event.preventDefault();
    saisie.ouvrir();
}

onMounted(() => window.addEventListener("keydown", surTouche));
onUnmounted(() => window.removeEventListener("keydown", surTouche));

watch(
    () => saisie.ouverte,
    async (ouverte) => {
        if (!ouverte) return;
        reinitialiser(true);
        if (saisie.aModifier) await preremplir(saisie.aModifier);
        await nextTick();
        champMontant.value?.focus();
        await chargerReferentiels();
    }
);

// Modification : le formulaire reprend l'opération telle qu'elle est
async function preremplir({ operation, libelle }: OperationAModifier) {
    montantTexte.value = String(operation.montant).replace(".", ",");
    if (libelle) {
        libelleExistant.value = libelle;
        libelleTexte.value = libelle.nom;
    }
    date.value = operation.date;
    compteId.value = operation.compteId;
    note.value = operation.note ?? "";
    // Après la mise à jour qui suit le choix du libellé, qui propose sa catégorie par défaut :
    // l'opération garde la sienne, même si elle a été changée à la saisie
    await nextTick();
    categorieId.value = operation.categorieId;
}

const enModification = computed(() => saisie.aModifier !== null);

// Rechargés à chaque ouverture : un compte ou une catégorie a pu être créé entre-temps
async function chargerReferentiels() {
    chargementReferentiels.value = true;
    try {
        const [listeComptes, listeCategories] = await Promise.all([
            comptesService.getComptes(),
            comptesService.getCategories(),
        ]);
        // Un compte ou une catégorie archivé(e) ou masqué(e) reste proposé(e) s'il est déjà celui de l'opération modifiée
        const actuelle = saisie.aModifier?.operation;
        comptes.value = listeComptes.filter(
            (c) => !c.archive || c.id === actuelle?.compteId
        );
        categories.value = listeCategories.filter(
            (c) => (!c.masquee && !c.archive) || c.id === actuelle?.categorieId
        );
        // Compte par défaut : celui de la dernière opération saisie
        if (
            compteId.value === null ||
            !comptes.value.some((c) => c.id === compteId.value)
        ) {
            const parDefaut =
                comptes.value.find((c) => c.dernierUtilise) ?? comptes.value[0];
            compteId.value = parDefaut?.id ?? null;
        }
    } catch (e: any) {
        erreur.value = messageErreur(e, "Impossible de charger vos comptes.");
    } finally {
        chargementReferentiels.value = false;
    }
}

// Complète : remet tout à zéro. Sinon (« en ajouter une autre ») : garde la date et le compte
function reinitialiser(complete: boolean) {
    montantTexte.value = "";
    libelleTexte.value = "";
    libelleExistant.value = null;
    categorieId.value = null;
    note.value = "";
    erreur.value = null;
    if (complete) {
        date.value = versIso(new Date());
        compteId.value = null;
    }
}

function fermer() {
    saisie.fermer();
}

const categoriesDepenses = computed(() =>
    categories.value.filter((c) => c.type === "DEPENSE")
);
const categoriesRevenus = computed(() =>
    categories.value.filter((c) => c.type === "REVENU")
);

// ── Libellé ──

const nouveauLibelle = computed(
    () => libelleTexte.value.trim() !== "" && libelleExistant.value === null
);

// Libellé reconnu : sa catégorie est proposée (elle reste modifiable ensuite)
watch(libelleExistant, (libelle) => {
    if (libelle) {
        categorieId.value = libelle.categorieId;
    }
});

// ── Enregistrement ──

// Montant en euros, virgule ou point, deux décimales au plus
function lireMontant(texte: string): number | null {
    const nettoye = texte.replace(/\s/g, "").replace(",", ".");
    if (!/^\d+(\.\d{1,2})?$/.test(nettoye)) return null;
    const montant = Number(nettoye);
    return montant > 0 ? montant : null;
}

async function enregistrer(enAjouterUneAutre: boolean) {
    if (enregistrementEnCours.value) return;
    erreur.value = null;

    const montant = lireMontant(montantTexte.value);
    if (montant === null) {
        erreur.value =
            "Le montant doit être un nombre positif, avec au plus deux décimales.";
        champMontant.value?.focus();
        return;
    }
    if (compteId.value === null) {
        erreur.value = "Choisissez un compte.";
        return;
    }
    if (categorieId.value === null) {
        erreur.value = nouveauLibelle.value
            ? `Choisissez la catégorie du nouveau libellé « ${libelleTexte.value.trim()} ».`
            : "Choisissez un libellé ou une catégorie.";
        return;
    }

    enregistrementEnCours.value = true;
    try {
        let libelleId = libelleExistant.value?.id ?? null;
        if (nouveauLibelle.value) {
            const cree = await comptesService.creerLibelle(
                libelleTexte.value.trim(),
                categorieId.value
            );
            libelleId = cree.id;
            // Le libellé existe maintenant : une nouvelle tentative ne le recréera pas
            libelleExistant.value = cree;
        }
        const valeurs = {
            compteId: compteId.value,
            date: date.value,
            montant,
            categorieId: categorieId.value,
            libelleId,
            note: note.value.trim() || null,
        };
        if (saisie.aModifier) {
            await comptesService.modifierOperation(
                saisie.aModifier.operation.id,
                valeurs
            );
        } else {
            await comptesService.creerOperation(valeurs);
        }
        saisie.signalerEnregistrement();
        notification.success(
            enModification.value
                ? "Opération modifiée."
                : "Opération enregistrée."
        );

        if (enAjouterUneAutre) {
            reinitialiser(false);
            await nextTick();
            champMontant.value?.focus();
        } else {
            fermer();
        }
    } catch (e: any) {
        erreur.value = messageErreur(
            e,
            "Impossible d'enregistrer l'opération."
        );
    } finally {
        enregistrementEnCours.value = false;
    }
}
</script>

<template>
    <!-- Seul bouton doré de l'écran : centré, collant en bas de page -->
    <div class="cta-bas">
        <button
            type="button"
            class="btn btn-principal"
            title="Nouvelle opération (N)"
            @click="saisie.ouvrir()"
        >
            <Plus aria-hidden="true" />
            Nouvelle opération
        </button>
    </div>

    <Teleport to="body">
        <div v-if="saisie.ouverte" class="voile" @mousedown.self="fermer">
            <div
                class="fenetre"
                role="dialog"
                aria-modal="true"
                aria-labelledby="titre-saisie"
            >
                <h2 id="titre-saisie" class="titre-fenetre">
                    {{
                        enModification
                            ? "Modifier l'opération"
                            : "Nouvelle opération"
                    }}
                </h2>

                <form @submit.prevent="enregistrer(false)">
                    <label class="champ">
                        Montant
                        <span class="champ-montant">
                            <input
                                ref="champMontant"
                                v-model="montantTexte"
                                type="text"
                                inputmode="decimal"
                                autocomplete="off"
                                placeholder="0,00"
                                required
                            />
                            <span class="devise" aria-hidden="true">€</span>
                        </span>
                    </label>

                    <ChampLibelleComponent
                        id="saisie-libelle"
                        v-model:texte="libelleTexte"
                        v-model:libelle="libelleExistant"
                    />

                    <label class="champ">
                        Catégorie
                        <select v-model="categorieId" required>
                            <option :value="null" disabled>
                                {{
                                    chargementReferentiels
                                        ? "Chargement..."
                                        : "Choisir une catégorie"
                                }}
                            </option>
                            <optgroup label="Dépenses">
                                <option
                                    v-for="c in categoriesDepenses"
                                    :key="c.id"
                                    :value="c.id"
                                >
                                    {{ c.nom }}
                                </option>
                            </optgroup>
                            <optgroup label="Revenus">
                                <option
                                    v-for="c in categoriesRevenus"
                                    :key="c.id"
                                    :value="c.id"
                                >
                                    {{ c.nom }}
                                </option>
                            </optgroup>
                        </select>
                    </label>

                    <div class="deux-colonnes">
                        <label class="champ">
                            Date
                            <input v-model="date" type="date" required />
                        </label>

                        <label class="champ">
                            Compte
                            <select v-model="compteId" required>
                                <option
                                    v-if="comptes.length === 0"
                                    :value="null"
                                    disabled
                                >
                                    {{
                                        chargementReferentiels
                                            ? "Chargement..."
                                            : "Aucun compte"
                                    }}
                                </option>
                                <option
                                    v-for="c in comptes"
                                    :key="c.id"
                                    :value="c.id"
                                >
                                    {{ c.nom }}
                                </option>
                            </select>
                        </label>
                    </div>

                    <label class="champ">
                        Note
                        <input
                            v-model="note"
                            type="text"
                            maxlength="255"
                            placeholder="Facultatif"
                        />
                    </label>

                    <p v-if="erreur" class="erreur" role="alert">
                        <CircleAlert aria-hidden="true" />
                        {{ erreur }}
                    </p>

                    <div class="pied-fenetre">
                        <button
                            type="button"
                            class="btn btn-discret"
                            @click="fermer"
                        >
                            Annuler
                        </button>
                        <button
                            type="button"
                            class="btn btn-secondaire"
                            v-if="!enModification"
                            :disabled="enregistrementEnCours"
                            @click="enregistrer(true)"
                        >
                            Enregistrer et en ajouter une autre
                        </button>
                        <button
                            type="submit"
                            class="btn btn-principal"
                            :disabled="enregistrementEnCours"
                        >
                            Enregistrer
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.cta-bas {
    position: sticky;
    bottom: 24px;
    z-index: 800;
    display: flex;
    justify-content: center;
    margin-bottom: 24px;
    padding: 0 16px;
    pointer-events: none;
}

.cta-bas .btn {
    pointer-events: auto;
    box-shadow: var(--ombre-flottante);
}

/* Mobile : toute la largeur, moins 16 px de chaque côté */
@media (max-width: 640px) {
    .cta-bas {
        bottom: 16px;
        margin-bottom: 16px;
    }

    .cta-bas .btn {
        width: 100%;
    }
}
</style>
