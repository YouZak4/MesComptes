<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
    LayoutDashboard,
    List,
    LogOut,
    Repeat,
    SlidersVertical,
    TrendingUp,
} from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";
import { useProfilStore } from "@/stores/profil";
import { nomAffiche } from "@/utils/utilisateur.ts";

const router = useRouter();
const auth = useAuthStore();
const notification = useNotificationStore();
const profil = useProfilStore();

const onglets = [
    { route: "accueil", titre: "Tableau de bord", icone: LayoutDashboard },
    { route: "operations", titre: "Opérations", icone: List },
    { route: "recurrences", titre: "Récurrences", icone: Repeat },
    { route: "reglages", titre: "Réglages", icone: SlidersVertical },
] as const;

// Dans le store : un changement de nom dans les réglages s'affiche tout de suite
const nom = computed(() =>
    profil.utilisateur ? nomAffiche(profil.utilisateur) : null
);
const initiale = computed(() => nom.value?.trim().charAt(0).toUpperCase());

onMounted(async () => {
    try {
        await profil.charger();
    } catch (error) {
        // Le nom n'est qu'un confort : l'en-tête reste utilisable sans
        console.error(error);
    }
});

// replace (et non push) : le bouton "Précédent" ne ramène pas sur la page protégée
async function deconnexion() {
    auth.logout();
    profil.vider();
    notification.success("Vous êtes déconnecté.");
    await router.replace({ name: "login" });
}
</script>

<template>
    <header class="barre">
        <div class="barre-contenu">
            <RouterLink :to="{ name: 'accueil' }" class="marque">
                <span class="logo" aria-hidden="true">
                    <TrendingUp :size="18" :stroke-width="2.2" />
                </span>
                Mes comptes
            </RouterLink>

            <nav class="onglets" aria-label="Navigation principale">
                <!-- Classe posée par RouterLink quand la route correspond exactement -->
                <RouterLink
                    v-for="o in onglets"
                    :key="o.route"
                    :to="{ name: o.route }"
                    class="onglet"
                    exact-active-class="actif"
                >
                    <component :is="o.icone" :size="18" aria-hidden="true" />
                    <span class="titre-onglet">{{ o.titre }}</span>
                </RouterLink>
            </nav>

            <div class="utilisateur">
                <span v-if="nom" class="avatar" aria-hidden="true">{{
                    initiale
                }}</span>
                <span v-if="nom" class="nom">{{ nom }}</span>
                <button
                    type="button"
                    class="btn btn-icone deconnexion"
                    aria-label="Déconnexion"
                    title="Déconnexion"
                    @click="deconnexion"
                >
                    <LogOut aria-hidden="true" />
                </button>
            </div>
        </div>
    </header>
</template>

<style scoped>
.barre {
    position: sticky;
    top: 0;
    z-index: 900;
    background: var(--vert-700);
    color: #e3f0e9;
}

.barre-contenu {
    max-width: var(--largeur-contenu);
    min-height: 64px;
    margin: 0 auto;
    padding: 10px 24px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 24px;
}

.marque {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #ffffff;
    font-size: 17px;
    font-weight: 800;
    letter-spacing: -0.01em;
}

.marque:hover {
    color: #ffffff;
    text-decoration: none;
}

.logo {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: var(--rayon-s);
    background: var(--vert-200);
    color: var(--vert-700);
}

.onglets {
    flex: 1 1 auto;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
}

.onglet {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 44px;
    padding: 0 14px;
    border-radius: var(--rayon-m);
    color: var(--vert-300);
    font-weight: 600;
}

.onglet:hover {
    background: var(--vert-800);
    color: #ffffff;
    text-decoration: none;
}

.onglet.actif {
    background: var(--vert-50);
    color: var(--vert-700);
    font-weight: 700;
}

.onglet:focus-visible,
.deconnexion:focus-visible {
    outline-color: var(--vert-200);
}

.utilisateur {
    display: flex;
    align-items: center;
    gap: 10px;
}

.avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--vert-200);
    color: var(--vert-700);
    font-weight: 800;
}

.nom {
    color: #ffffff;
    font-weight: 700;
}

.deconnexion {
    color: var(--vert-300);
}

.deconnexion:hover:not(:disabled) {
    background: var(--vert-800);
    color: #ffffff;
}

/* Mobile : onglets en icônes seules, sur leur propre ligne */
@media (max-width: 640px) {
    .barre-contenu {
        padding: 8px 16px;
        gap: 8px 12px;
    }

    .utilisateur {
        margin-left: auto;
    }

    .nom {
        display: none;
    }

    .onglets {
        order: 3;
        flex-basis: 100%;
        justify-content: space-between;
    }

    .onglet {
        flex: 1;
        justify-content: center;
        padding: 0 8px;
    }

    /* Masqué à l'écran, toujours lu par les lecteurs d'écran */
    .titre-onglet {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip-path: inset(50%);
        white-space: nowrap;
    }
}
</style>
