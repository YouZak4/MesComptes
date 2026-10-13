<script setup lang="ts">
import { onMounted, ref } from "vue";
import { utilisateurService } from "@/services/utilisateurService.ts";
import { useNotificationStore } from "@/stores/notification";
import { useProfilStore } from "@/stores/profil";
import { messageErreur } from "@/utils/erreurs.ts";

const notification = useNotificationStore();
const profil = useProfilStore();

const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

const pseudo = ref("");
const erreurPseudo = ref<string | null>(null);
const pseudoEnCours = ref(false);

onMounted(async () => {
    try {
        const moi = profil.utilisateur ?? (await profil.charger());
        pseudo.value = moi.pseudo;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger votre profil."
        );
    } finally {
        isLoading.value = false;
    }
});

async function enregistrerPseudo() {
    if (pseudoEnCours.value) return;
    pseudoEnCours.value = true;
    erreurPseudo.value = null;
    try {
        const modifie = await utilisateurService.modifierProfil(
            pseudo.value.trim()
        );
        profil.mettreAJour(modifie);
        pseudo.value = modifie.pseudo;
        notification.success("Pseudonyme modifié.");
    } catch (e: any) {
        erreurPseudo.value = messageErreur(
            e,
            "Impossible de modifier le pseudonyme."
        );
    } finally {
        pseudoEnCours.value = false;
    }
}

// ── Mot de passe ──

const actuel = ref("");
const nouveau = ref("");
const confirmation = ref("");
const erreurMotDePasse = ref<string | null>(null);
const motDePasseEnCours = ref(false);

async function changerMotDePasse() {
    if (motDePasseEnCours.value) return;
    erreurMotDePasse.value = null;
    if (nouveau.value !== confirmation.value) {
        erreurMotDePasse.value =
            "La confirmation ne correspond pas au nouveau mot de passe.";
        return;
    }
    motDePasseEnCours.value = true;
    try {
        await utilisateurService.changerMotDePasse(actuel.value, nouveau.value);
        actuel.value = "";
        nouveau.value = "";
        confirmation.value = "";
        notification.success("Mot de passe changé.");
    } catch (e: any) {
        erreurMotDePasse.value = messageErreur(
            e,
            "Impossible de changer le mot de passe."
        );
    } finally {
        motDePasseEnCours.value = false;
    }
}
</script>

<template>
    <p v-if="isLoading" class="message">Chargement...</p>
    <p v-else-if="errorMessage" class="message erreur">{{ errorMessage }}</p>

    <template v-else>
        <section class="carte">
            <h2>Profil</h2>
            <form class="formulaire" @submit.prevent="enregistrerPseudo">
                <label class="champ large">
                    Pseudonyme
                    <input
                        v-model="pseudo"
                        type="text"
                        maxlength="255"
                        autocomplete="nickname"
                        required
                    />
                </label>
                <button
                    type="submit"
                    class="bouton-principal"
                    :disabled="
                        pseudoEnCours ||
                        pseudo.trim() === profil.utilisateur?.pseudo
                    "
                >
                    Enregistrer
                </button>
                <p v-if="erreurPseudo" class="erreur" role="alert">
                    {{ erreurPseudo }}
                </p>
            </form>
            <p class="email">
                Adresse mail : <strong>{{ profil.utilisateur?.email }}</strong>
            </p>
            <p class="aide">
                L'adresse mail ne se modifie pas encore : il faudra la vérifier
                par un nouveau code.
            </p>
        </section>

        <section class="carte">
            <h2>Mot de passe</h2>
            <form class="formulaire" @submit.prevent="changerMotDePasse">
                <label class="champ large">
                    Mot de passe actuel
                    <input
                        v-model="actuel"
                        type="password"
                        autocomplete="current-password"
                        required
                    />
                </label>
                <label class="champ large">
                    Nouveau mot de passe
                    <input
                        v-model="nouveau"
                        type="password"
                        autocomplete="new-password"
                        required
                    />
                </label>
                <label class="champ large">
                    Confirmation
                    <input
                        v-model="confirmation"
                        type="password"
                        autocomplete="new-password"
                        required
                    />
                </label>
                <button
                    type="submit"
                    class="bouton-principal"
                    :disabled="motDePasseEnCours"
                >
                    Changer le mot de passe
                </button>
                <p v-if="erreurMotDePasse" class="erreur" role="alert">
                    {{ erreurMotDePasse }}
                </p>
            </form>
        </section>
    </template>
</template>

<style scoped>
.erreur {
    flex-basis: 100%;
}

.email {
    margin: 1rem 0 0.25rem;
}
</style>
