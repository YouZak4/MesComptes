<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { comptesService } from "@/services/comptesService.ts";
import { useNotificationStore } from "@/stores/notification";
import { useSaisieStore } from "@/stores/saisie";
import type { Categorie, Compte, Libelle } from "@/types/comptes.ts";
import { versIso } from "@/utils/format.ts";

const saisie = useSaisieStore();
const notification = useNotificationStore();

const comptes = ref<Compte[]>([]);
const categories = ref<Categorie[]>([]);
const chargementReferentiels = ref(false);
const enregistrementEnCours = ref(false);
const erreur = ref<string | null>(null);

const montantTexte = ref("");
const libelleTexte = ref("");
const libelleChoisi = ref<Libelle | null>(null);
const categorieId = ref<number | null>(null);
const date = ref(versIso(new Date()));
const compteId = ref<number | null>(null);
const note = ref("");

const champMontant = ref<HTMLInputElement | null>(null);

// ── Ouverture : touche N ou bouton « + » ──

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
        await nextTick();
        champMontant.value?.focus();
        await chargerReferentiels();
    }
);

// Rechargés à chaque ouverture : un compte ou une catégorie a pu être créé entre-temps
async function chargerReferentiels() {
    chargementReferentiels.value = true;
    try {
        const [listeComptes, listeCategories] = await Promise.all([
            comptesService.getComptes(),
            comptesService.getCategories(),
        ]);
        comptes.value = listeComptes.filter((c) => !c.archive);
        categories.value = listeCategories.filter(
            (c) => !c.masquee && !c.archive
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
    libelleChoisi.value = null;
    categorieId.value = null;
    note.value = "";
    erreur.value = null;
    suggestions.value = [];
    fermerSuggestions();
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

// ── Autocomplétion du libellé ──

const suggestions = ref<Libelle[]>([]);
const suggestionsOuvertes = ref(false);
const suggestionActive = ref(-1);
let minuterieRecherche: ReturnType<typeof setTimeout> | undefined;
// Seule la réponse à la dernière recherche compte, même si une plus ancienne arrive après
let numeroRecherche = 0;

// Même règle que le back : sans tenir compte des majuscules ni des espaces en trop
function normaliser(nom: string): string {
    return nom.trim().replace(/\s+/g, " ").toLowerCase();
}

function surSaisieLibelle() {
    libelleChoisi.value = null;
    clearTimeout(minuterieRecherche);
    const texte = libelleTexte.value.trim();
    if (texte === "") {
        suggestions.value = [];
        fermerSuggestions();
        return;
    }
    minuterieRecherche = setTimeout(() => rechercher(texte), 150);
}

async function rechercher(texte: string) {
    const numero = ++numeroRecherche;
    try {
        const resultats = await comptesService.rechercherLibelles(texte);
        if (numero !== numeroRecherche) return;
        suggestions.value = resultats;
        suggestionActive.value = resultats.length > 0 ? 0 : -1;
        suggestionsOuvertes.value = resultats.length > 0;
    } catch (e) {
        // Sans suggestions, la saisie reste possible (le libellé sera créé)
        console.error(e);
    }
}

function choisirLibelle(libelle: Libelle) {
    libelleChoisi.value = libelle;
    libelleTexte.value = libelle.nom;
    // La catégorie du libellé est proposée, et reste modifiable
    categorieId.value = libelle.categorieId;
    fermerSuggestions();
}

function fermerSuggestions() {
    suggestionsOuvertes.value = false;
    suggestionActive.value = -1;
}

function surToucheLibelle(event: KeyboardEvent) {
    if (!suggestionsOuvertes.value) return;
    const nb = suggestions.value.length;
    if (event.key === "ArrowDown") {
        event.preventDefault();
        suggestionActive.value = (suggestionActive.value + 1) % nb;
    } else if (event.key === "ArrowUp") {
        event.preventDefault();
        suggestionActive.value = (suggestionActive.value - 1 + nb) % nb;
    } else if (event.key === "Enter" && suggestionActive.value >= 0) {
        // Entrée choisit la suggestion ; une seconde Entrée enregistrera
        event.preventDefault();
        choisirLibelle(suggestions.value[suggestionActive.value]);
    } else if (event.key === "Escape") {
        // Échap ferme d'abord la liste, puis la fenêtre
        event.preventDefault();
        event.stopPropagation();
        fermerSuggestions();
    }
}

// Libellé tapé qui correspond exactement à une suggestion, sans l'avoir choisie dans la liste
const libelleExistant = computed<Libelle | null>(() => {
    if (libelleChoisi.value) return libelleChoisi.value;
    const nom = normaliser(libelleTexte.value);
    if (nom === "") return null;
    return suggestions.value.find((l) => normaliser(l.nom) === nom) ?? null;
});

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

function messageErreur(e: any, parDefaut: string): string {
    return e.response?.data?.message || parDefaut;
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
            choisirLibelle(cree);
        }
        await comptesService.creerOperation({
            compteId: compteId.value,
            date: date.value,
            montant,
            categorieId: categorieId.value,
            libelleId,
            note: note.value.trim() || null,
        });
        saisie.signalerEnregistrement();
        notification.success("Opération enregistrée.");

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
    <button
        type="button"
        class="bouton-ajout"
        title="Nouvelle opération (N)"
        aria-label="Nouvelle opération"
        @click="saisie.ouvrir()"
    >
        +
    </button>

    <Teleport to="body">
        <div v-if="saisie.ouverte" class="fond" @mousedown.self="fermer">
            <div
                class="fenetre"
                role="dialog"
                aria-modal="true"
                aria-labelledby="titre-saisie"
            >
                <h2 id="titre-saisie">Nouvelle opération</h2>

                <form @submit.prevent="enregistrer(false)">
                    <label class="champ">
                        Montant
                        <span class="montant">
                            <input
                                ref="champMontant"
                                v-model="montantTexte"
                                type="text"
                                inputmode="decimal"
                                autocomplete="off"
                                placeholder="0,00"
                                required
                            />
                            <span class="devise">€</span>
                        </span>
                    </label>

                    <div class="champ libelle">
                        <label for="saisie-libelle">Libellé</label>
                        <!-- Conteneur de la liste : elle s'ouvre juste sous le champ, par-dessus le reste -->
                        <div class="zone-libelle">
                            <input
                                id="saisie-libelle"
                                v-model="libelleTexte"
                                type="text"
                                autocomplete="off"
                                placeholder="Netflix, Supermarché…"
                                maxlength="100"
                                role="combobox"
                                aria-autocomplete="list"
                                aria-controls="saisie-suggestions"
                                :aria-expanded="suggestionsOuvertes"
                                @input="surSaisieLibelle"
                                @keydown="surToucheLibelle"
                                @blur="fermerSuggestions"
                            />
                            <ul
                                v-if="suggestionsOuvertes"
                                id="saisie-suggestions"
                                class="suggestions"
                                role="listbox"
                            >
                                <!-- mousedown (et non click) : passe avant le blur qui ferme la liste -->
                                <li
                                    v-for="(libelle, index) in suggestions"
                                    :key="libelle.id"
                                    role="option"
                                    :aria-selected="index === suggestionActive"
                                    :class="{
                                        active: index === suggestionActive,
                                    }"
                                    @mousedown.prevent="choisirLibelle(libelle)"
                                >
                                    {{ libelle.nom }}
                                </li>
                            </ul>
                        </div>
                        <p
                            v-if="nouveauLibelle && !suggestionsOuvertes"
                            class="aide"
                        >
                            Nouveau libellé : il sera créé avec la catégorie
                            choisie.
                        </p>
                    </div>

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
                                    {{ c.icone }} {{ c.nom }}
                                </option>
                            </optgroup>
                            <optgroup label="Revenus">
                                <option
                                    v-for="c in categoriesRevenus"
                                    :key="c.id"
                                    :value="c.id"
                                >
                                    {{ c.icone }} {{ c.nom }}
                                </option>
                            </optgroup>
                        </select>
                    </label>

                    <div class="ligne">
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
                        {{ erreur }}
                    </p>

                    <div class="actions">
                        <button
                            type="button"
                            class="bouton-secondaire"
                            @click="fermer"
                        >
                            Annuler
                        </button>
                        <button
                            type="button"
                            class="bouton-secondaire"
                            :disabled="enregistrementEnCours"
                            @click="enregistrer(true)"
                        >
                            Enregistrer et en ajouter une autre
                        </button>
                        <button
                            type="submit"
                            class="bouton-principal"
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
.bouton-ajout {
    position: fixed;
    right: 1.5rem;
    bottom: 1.5rem;
    z-index: 900;
    width: 3.5rem;
    height: 3.5rem;
    border: none;
    border-radius: 50%;
    background: #45335c;
    color: #ffffff;
    font-size: 2rem;
    line-height: 1;
    box-shadow: 0 4px 12px rgba(69, 51, 92, 0.35);
    cursor: pointer;
}

.bouton-ajout:hover {
    background: #5a4377;
}

.fond {
    position: fixed;
    inset: 0;
    z-index: 950;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: 8vh 1rem 1rem;
    background: rgba(30, 20, 45, 0.45);
    overflow-y: auto;
}

.fenetre {
    width: min(100%, 480px);
    padding: 1.5rem;
    border-radius: 12px;
    background: #ffffff;
    color: #45335c;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
}

h2 {
    margin: 0 0 1rem;
    font-size: 1.25rem;
}

form {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.champ {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    min-width: 0;
}

.ligne {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.75rem;
}

input,
select {
    padding: 0.55rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #000000;
    font-size: 0.95rem;
    font-family: inherit;
    min-width: 0;
}

input:focus,
select:focus {
    outline: 2px solid #45335c;
    outline-offset: 1px;
}

.montant {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.montant input {
    flex: 1;
    font-size: 1.5rem;
    font-weight: bold;
}

.devise {
    font-size: 1.5rem;
    font-weight: bold;
}

.zone-libelle {
    position: relative;
    display: flex;
    flex-direction: column;
}

.suggestions {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 1;
    margin: 0.25rem 0 0;
    padding: 0.25rem 0;
    list-style: none;
    max-height: 14rem;
    overflow-y: auto;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #ffffff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.suggestions li {
    padding: 0.45rem 0.75rem;
    cursor: pointer;
}

.suggestions li.active {
    background: #e3d3f1;
}

.aide {
    margin: 0;
    font-size: 0.85rem;
    color: #81758e;
}

.erreur {
    margin: 0;
    color: #c62727;
}

.actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 0.5rem;
    margin-top: 0.25rem;
}

.bouton-principal,
.bouton-secondaire {
    padding: 0.6rem 1rem;
    border-radius: 10px;
    font-size: 0.95rem;
    cursor: pointer;
}

.bouton-principal {
    border: none;
    background: #45335c;
    color: #ffffff;
}

.bouton-secondaire {
    border: 1px solid #45335c;
    background: transparent;
    color: #45335c;
}

.bouton-principal:disabled,
.bouton-secondaire:disabled {
    opacity: 0.6;
    cursor: default;
}
</style>
