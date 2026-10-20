<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import ReglagesComptesComponent from "@/components/reglages/ReglagesComptesComponent.vue";
import ReglagesPeriodesComponent from "@/components/reglages/ReglagesPeriodesComponent.vue";
import ReglagesCategoriesComponent from "@/components/reglages/ReglagesCategoriesComponent.vue";
import ReglagesProfilComponent from "@/components/reglages/ReglagesProfilComponent.vue";
import "@/styles/reglages.css";

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

    <main class="page page-etroite reglages">
        <h1 class="titre-page">Réglages</h1>

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
