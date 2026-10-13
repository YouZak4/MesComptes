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
body {
    margin: 0;
    font-family: Arial, Helvetica, sans-serif;
}
</style>
