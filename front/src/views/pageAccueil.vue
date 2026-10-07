<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { utilisateurService } from "@/services/utilisateurService.ts";
import type { Utilisateur } from "@/types/utilisateur.ts";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";

const router = useRouter();
const auth = useAuthStore();
const notification = useNotificationStore();

const utilisateurs = ref<Utilisateur[]>([]);
const isLoading = ref(false);
const errorMessage = ref<string | null>(null);

onMounted(async () => {
    isLoading.value = true;
    try {
        utilisateurs.value = await utilisateurService.getAllUsers();
    } catch (error) {
        errorMessage.value = "Impossible de charger les utilisateurs.";
        console.error(error);
    } finally {
        isLoading.value = false;
    }
});

// replace (et non push) : le bouton "Précédent" ne ramène pas sur la page protégée
async function deconnexion() {
    auth.logout();
    notification.success("Vous êtes déconnecté.");
    await router.replace({ name: "login" });
}
</script>

<template>
    <main>
        <button type="button" @click="deconnexion">Deconnexion</button>

        <h1>Liste des utilisateurs</h1>

        <p v-if="isLoading">Chargement...</p>

        <p v-else-if="errorMessage" style="color: red">
            {{ errorMessage }}
        </p>

        <ul v-else-if="utilisateurs.length > 0">
            <li v-for="user in utilisateurs" :key="user.id">
                <strong>{{ user.name }}</strong> — {{ user.email }}
            </li>
        </ul>

        <p v-else>Aucun utilisateur trouvé.</p>
    </main>
</template>
