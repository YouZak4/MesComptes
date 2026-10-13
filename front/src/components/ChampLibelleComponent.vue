<script setup lang="ts">
import { ref, watch } from "vue";
import { comptesService } from "@/services/comptesService.ts";
import type { Libelle } from "@/types/comptes.ts";

// Champ « Libellé » avec autocomplétion. Le parent reçoit le texte tapé et le libellé existant
// qu'il désigne (choisi dans la liste, ou tapé exactement), null pour un nouveau libellé
const props = defineProps<{ id: string }>();
const texte = defineModel<string>("texte", { required: true });
const libelle = defineModel<Libelle | null>("libelle", { required: true });

const suggestions = ref<Libelle[]>([]);
const suggestionsOuvertes = ref(false);
const suggestionActive = ref(-1);
let minuterieRecherche: ReturnType<typeof setTimeout> | undefined;
// Seule la réponse à la dernière recherche compte, même si une plus ancienne arrive après
let numeroRecherche = 0;

const idListe = `${props.id}-suggestions`;

// Même règle que le back : sans tenir compte des majuscules ni des espaces en trop
function normaliser(nom: string): string {
    return nom.trim().replace(/\s+/g, " ").toLowerCase();
}

// Libellé tapé qui correspond exactement à une suggestion, sans l'avoir choisi dans la liste
function reconnaitre() {
    const nom = normaliser(texte.value);
    libelle.value =
        nom === ""
            ? null
            : (suggestions.value.find((l) => normaliser(l.nom) === nom) ??
              null);
}

function surSaisie() {
    reconnaitre();
    clearTimeout(minuterieRecherche);
    const recherche = texte.value.trim();
    if (recherche === "") {
        suggestions.value = [];
        fermerSuggestions();
        return;
    }
    minuterieRecherche = setTimeout(() => rechercher(recherche), 150);
}

async function rechercher(recherche: string) {
    const numero = ++numeroRecherche;
    try {
        const resultats = await comptesService.rechercherLibelles(recherche);
        if (numero !== numeroRecherche) return;
        suggestions.value = resultats;
        suggestionActive.value = resultats.length > 0 ? 0 : -1;
        suggestionsOuvertes.value = resultats.length > 0;
        if (libelle.value === null) reconnaitre();
    } catch (e) {
        // Sans suggestions, la saisie reste possible (le libellé sera créé)
        console.error(e);
    }
}

function choisir(choisi: Libelle) {
    libelle.value = choisi;
    texte.value = choisi.nom;
    fermerSuggestions();
}

function fermerSuggestions() {
    suggestionsOuvertes.value = false;
    suggestionActive.value = -1;
}

// Champ vidé par le parent (remise à zéro du formulaire) : plus de suggestions
watch(texte, (valeur) => {
    if (valeur === "") {
        clearTimeout(minuterieRecherche);
        numeroRecherche++;
        suggestions.value = [];
        fermerSuggestions();
    }
});

function surTouche(event: KeyboardEvent) {
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
        choisir(suggestions.value[suggestionActive.value]);
    } else if (event.key === "Escape") {
        // Échap ferme d'abord la liste, puis la fenêtre
        event.preventDefault();
        event.stopPropagation();
        fermerSuggestions();
    }
}
</script>

<template>
    <div class="champ-libelle">
        <label :for="id">Libellé</label>
        <!-- Conteneur de la liste : elle s'ouvre juste sous le champ, par-dessus le reste -->
        <div class="zone">
            <input
                :id="id"
                v-model="texte"
                type="text"
                autocomplete="off"
                placeholder="Netflix, Supermarché…"
                maxlength="100"
                role="combobox"
                aria-autocomplete="list"
                :aria-controls="idListe"
                :aria-expanded="suggestionsOuvertes"
                @input="surSaisie"
                @keydown="surTouche"
                @blur="fermerSuggestions"
            />
            <ul
                v-if="suggestionsOuvertes"
                :id="idListe"
                class="suggestions"
                role="listbox"
            >
                <!-- mousedown (et non click) : passe avant le blur qui ferme la liste -->
                <li
                    v-for="(suggestion, index) in suggestions"
                    :key="suggestion.id"
                    role="option"
                    :aria-selected="index === suggestionActive"
                    :class="{ active: index === suggestionActive }"
                    @mousedown.prevent="choisir(suggestion)"
                >
                    {{ suggestion.nom }}
                </li>
            </ul>
        </div>
        <p
            v-if="
                texte.trim() !== '' && libelle === null && !suggestionsOuvertes
            "
            class="aide"
        >
            Nouveau libellé : il sera créé avec la catégorie choisie.
        </p>
    </div>
</template>

<style scoped>
.champ-libelle {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    min-width: 0;
}

.zone {
    position: relative;
    display: flex;
    flex-direction: column;
}

input {
    padding: 0.55rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #000000;
    font-size: 0.95rem;
    font-family: inherit;
    min-width: 0;
}

input:focus {
    outline: 2px solid #45335c;
    outline-offset: 1px;
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
</style>
