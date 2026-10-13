<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { utilisateurService } from "@/services/utilisateurService.ts";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";

const router = useRouter();
const auth = useAuthStore();
const notification = useNotificationStore();

const pseudo = ref<string | null>(null);

onMounted(async () => {
    try {
        pseudo.value = (await utilisateurService.getMoi()).pseudo;
    } catch (error) {
        // Le pseudo n'est qu'un confort : l'en-tête reste utilisable sans
        console.error(error);
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
    <header class="en-tete">
        <RouterLink :to="{ name: 'accueil' }" class="logo">
            Mes comptes
        </RouterLink>
        <nav class="navigation">
            <RouterLink :to="{ name: 'accueil' }">Tableau de bord</RouterLink>
            <RouterLink :to="{ name: 'operations' }">Opérations</RouterLink>
        </nav>
        <div class="utilisateur">
            <span v-if="pseudo" class="pseudo">{{ pseudo }}</span>
            <button type="button" class="deconnexion" @click="deconnexion">
                Déconnexion
            </button>
        </div>
    </header>
</template>

<style scoped>
.en-tete {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1.5rem;
    background: #45335c;
    color: #ffffff;
}

.logo {
    font-size: 1.25rem;
    font-weight: bold;
    color: inherit;
    text-decoration: none;
}

.navigation {
    flex: 1;
    display: flex;
    gap: 0.25rem;
    margin-left: 2rem;
}

.navigation a {
    padding: 0.4rem 0.75rem;
    border-radius: 8px;
    color: #e3d3f1;
    text-decoration: none;
}

.navigation a:hover {
    background: rgba(255, 255, 255, 0.1);
}

/* Page en cours (classe posée par RouterLink quand la route correspond exactement) */
.navigation a.router-link-exact-active {
    background: rgba(255, 255, 255, 0.18);
    color: #ffffff;
}

.utilisateur {
    display: flex;
    align-items: center;
    gap: 1rem;
}

.deconnexion {
    padding: 0.4rem 0.9rem;
    border: 1px solid #e3d3f1;
    border-radius: 10px;
    background: transparent;
    color: inherit;
    cursor: pointer;
}

.deconnexion:hover {
    background: rgba(255, 255, 255, 0.1);
}
</style>
