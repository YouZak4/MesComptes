<script setup lang="ts">
import { useRoute } from "vue-router";
import NotificationComponent from "@/components/NotificationComponent.vue";
import SaisieRapideComponent from "@/components/SaisieRapideComponent.vue";
import { useAuthStore } from "@/stores/auth";

const route = useRoute();
const auth = useAuthStore();
</script>

<template>
    <RouterView />
    <!-- Saisie rapide accessible depuis tous les écrans connectés -->
    <SaisieRapideComponent
        v-if="auth.isAuthenticated && route.meta.requiresAuth"
    />
    <NotificationComponent />
</template>

<style>
/* Le bouton « Nouvelle opération », collant, reste en bas de l'écran même quand la page est courte */
#app {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

#app > .page {
    flex: 1 0 auto;
}
</style>
