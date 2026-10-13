<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import ReglagesComptesComponent from "@/components/reglages/ReglagesComptesComponent.vue";
import ReglagesPeriodesComponent from "@/components/reglages/ReglagesPeriodesComponent.vue";
import ReglagesCategoriesComponent from "@/components/reglages/ReglagesCategoriesComponent.vue";
import ReglagesProfilComponent from "@/components/reglages/ReglagesProfilComponent.vue";

const route = useRoute();
const router = useRouter();

const onglets = [
    { id: "comptes", titre: "Comptes" },
    { id: "periodes", titre: "Périodes" },
    { id: "categories", titre: "Catégories et libellés" },
    { id: "profil", titre: "Profil" },
] as const;
type Onglet = (typeof onglets)[number]["id"];

// Onglet dans l'URL : un rechargement ou un lien le garde
const onglet = computed<Onglet>(() => {
    const demande = route.query.onglet;
    return onglets.find((o) => o.id === demande)?.id ?? "comptes";
});

function choisir(id: Onglet) {
    router.replace({ query: { onglet: id } });
}
</script>

<template>
    <EnTeteComponent />

    <main class="reglages">
        <h1>Réglages</h1>

        <nav class="onglets" role="tablist">
            <button
                v-for="o in onglets"
                :key="o.id"
                type="button"
                role="tab"
                :aria-selected="onglet === o.id"
                :class="{ actif: onglet === o.id }"
                @click="choisir(o.id)"
            >
                {{ o.titre }}
            </button>
        </nav>

        <ReglagesComptesComponent v-if="onglet === 'comptes'" />
        <ReglagesPeriodesComponent v-else-if="onglet === 'periodes'" />
        <ReglagesCategoriesComponent v-else-if="onglet === 'categories'" />
        <ReglagesProfilComponent v-else />
    </main>
</template>

<!-- Non scoped : styles communs aux onglets (leurs composants sont dans .reglages) -->
<style>
.reglages {
    color: #45335c;
    max-width: 900px;
    margin: 0 auto;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.reglages h1 {
    margin: 0;
    font-size: 1.6rem;
}

.reglages h2 {
    margin: 0 0 0.75rem;
    font-size: 1.1rem;
}

.reglages .onglets {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
    border-bottom: 1px solid #e3d3f1;
}

.reglages .onglets button {
    padding: 0.6rem 1rem;
    border: none;
    border-bottom: 3px solid transparent;
    background: none;
    color: #81758e;
    font-size: 0.95rem;
    cursor: pointer;
}

.reglages .onglets button.actif {
    border-bottom-color: #45335c;
    color: #45335c;
    font-weight: bold;
}

.reglages .carte {
    background: #ffffff;
    border: 1px solid #e3d3f1;
    border-radius: 10px;
    padding: 1.25rem;
    box-shadow: 0 2px 6px rgba(69, 51, 92, 0.08);
}

.reglages .message {
    text-align: center;
}

.reglages .vide,
.reglages .aide {
    margin: 0;
    color: #81758e;
}

.reglages .aide {
    font-size: 0.85rem;
}

.reglages .erreur {
    margin: 0;
    color: #c62727;
}

.reglages .depense {
    color: #c62727;
}

.reglages .revenu {
    color: #14a81a;
}

/* Formulaires */
.reglages .formulaire {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: 0.75rem;
}

.reglages .champ {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    min-width: 0;
    font-size: 0.9rem;
}

.reglages .champ.large {
    flex: 1 1 200px;
}

.reglages input:not([type="color"]):not([type="checkbox"]),
.reglages select {
    padding: 0.5rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #000000;
    font-size: 0.95rem;
    font-family: inherit;
    min-width: 0;
}

.reglages input[type="color"] {
    width: 3rem;
    height: 2.4rem;
    padding: 0.15rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
}

.reglages input:focus,
.reglages select:focus {
    outline: 2px solid #45335c;
    outline-offset: 1px;
}

/* Listes */
.reglages ul.liste {
    list-style: none;
    margin: 0;
    padding: 0;
}

.reglages ul.liste > li {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0;
    border-bottom: 1px solid #f1e9f8;
}

.reglages ul.liste > li:last-child {
    border-bottom: none;
}

.reglages .texte {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.reglages .titre {
    font-weight: bold;
}

.reglages .details {
    font-size: 0.85rem;
    color: #81758e;
}

.reglages .inactif .titre,
.reglages .inactif .icone {
    opacity: 0.55;
}

.reglages .badge {
    display: inline-block;
    margin-left: 0.4rem;
    padding: 0.05rem 0.45rem;
    border-radius: 999px;
    background: #f1e9f8;
    color: #81758e;
    font-size: 0.75rem;
    font-weight: normal;
}

/* Boutons */
.reglages .actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.25rem;
}

.reglages .confirmation span {
    font-size: 0.85rem;
    margin-right: 0.25rem;
}

.reglages .bouton-principal,
.reglages .bouton-secondaire {
    padding: 0.5rem 0.9rem;
    border-radius: 10px;
    font-size: 0.95rem;
    cursor: pointer;
}

.reglages .bouton-principal {
    border: none;
    background: #45335c;
    color: #ffffff;
}

.reglages .bouton-principal:hover {
    background: #5a4377;
}

.reglages .bouton-secondaire {
    border: 1px solid #45335c;
    background: transparent;
    color: #45335c;
}

.reglages .bouton-action,
.reglages .bouton-danger {
    padding: 0.3rem 0.5rem;
    border: 1px solid transparent;
    border-radius: 8px;
    background: none;
    color: #45335c;
    cursor: pointer;
    font-size: 0.9rem;
}

.reglages .bouton-action:hover {
    border-color: #e3d3f1;
    background: #f7f2fb;
}

.reglages .bouton-danger {
    background: #c62727;
    color: #ffffff;
}

.reglages button:disabled {
    opacity: 0.5;
    cursor: default;
}
</style>
