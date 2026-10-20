<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/services/api";
import { useNotificationStore } from "@/stores/notification";
import { TrendingUp } from "@lucide/vue";
import InputComponent from "@/components/InputComponent.vue";
import "@/styles/connexion.css";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();

// Jeton opaque fourni par le back : l'e-mail complet ne transite plus côté front
const jeton = String(route.params.jeton);
const emailMasque = ref("");
const codeVerification = ref("");
const codeValide = ref(false);
// true jusqu'à la réponse du back : les boutons restent bloqués pendant le chargement
const isLoading = ref(true);

// Le délai vient toujours du back, qui connaît l'heure réelle du dernier envoi
const secondesAvantRenvoi = ref(0);
let minuteur: ReturnType<typeof setInterval> | undefined;

function demarrerMinuteur(secondes: number) {
    secondesAvantRenvoi.value = secondes;
    clearInterval(minuteur);
    if (secondes <= 0) return;
    minuteur = setInterval(() => {
        secondesAvantRenvoi.value--;
        if (secondesAvantRenvoi.value <= 0) clearInterval(minuteur);
    }, 1000);
}

onUnmounted(() => clearInterval(minuteur));

// Récupère l'adresse masquée et le délai restant, et vérifie au passage que le jeton est encore valide
onMounted(async () => {
    try {
        const { data } = await api.get(`/auth/register/${jeton}`);
        emailMasque.value = data.emailMasque;
        demarrerMinuteur(data.secondesAvantRenvoi);
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Ce lien de vérification n'est plus valide.")
        );
        await router.replace({ name: "login" });
    } finally {
        isLoading.value = false;
    }
});

async function renvoyerCode() {
    isLoading.value = true;
    try {
        const { data } = await api.post("/auth/register/resend", { jeton });
        notification.success("Un nouveau code vous a été envoyé par e-mail.");
        codeVerification.value = "";
        demarrerMinuteur(data.secondesAvantRenvoi);
    } catch (e: any) {
        notification.error(messageErreur(e, "Impossible de renvoyer le code."));
    } finally {
        isLoading.value = false;
    }
}

function messageErreur(e: any, parDefaut: string): string {
    return e.response?.data?.message || parDefaut;
}

async function verifier() {
    isLoading.value = true;
    try {
        await api.post("/auth/register/verify", {
            jeton,
            code: codeVerification.value,
        });
        notification.success(
            "Votre compte a été créé avec succès. Vous pouvez vous connecter."
        );
        // replace : on ne doit pas pouvoir revenir sur un code déjà utilisé
        await router.replace({ name: "login" });
    } catch (e: any) {
        notification.error(messageErreur(e, "Code invalide."));
        // 409 : identifiant ou adresse pris entre-temps, il faut recommencer l'inscription
        if (e.response?.status === 409) {
            await router.replace({ name: "login" });
        }
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
    <main class="connexion">
        <div class="carte carte-connexion">
            <div class="entete">
                <h1 class="marque">
                    <span class="logo" aria-hidden="true">
                        <TrendingUp :size="20" :stroke-width="2.2" />
                    </span>
                    Mes comptes
                </h1>
                <p class="accroche">Votre outil de suivi des dépenses</p>
            </div>

            <form @submit.prevent="verifier">
                <div>
                    <h2 class="titre-formulaire">
                        Vérifiez votre adresse e-mail
                    </h2>
                    <p class="consigne">
                        Saisissez le code à 6 chiffres envoyé à
                        {{ emailMasque }}
                    </p>
                </div>

                <InputComponent
                    v-model="codeVerification"
                    label="Code de vérification"
                    :required="true"
                    :regex="/^\d{6}$/"
                    regex-message="Le code contient 6 chiffres."
                    @valid="codeValide = $event"
                />

                <div class="boutons">
                    <button
                        type="submit"
                        class="btn btn-principal"
                        :disabled="!codeValide || isLoading"
                    >
                        Valider
                    </button>
                    <button
                        type="button"
                        class="btn btn-secondaire"
                        :disabled="secondesAvantRenvoi > 0 || isLoading"
                        @click="renvoyerCode"
                    >
                        {{
                            secondesAvantRenvoi > 0
                                ? `Renvoyer le code (${secondesAvantRenvoi} s)`
                                : "Renvoyer le code"
                        }}
                    </button>
                    <button
                        type="button"
                        class="btn btn-discret"
                        @click="router.replace({ name: 'login' })"
                    >
                        Retour à la connexion
                    </button>
                </div>
            </form>
        </div>
    </main>
</template>

<style scoped>
.consigne {
    margin-top: 4px;
    color: var(--encre-douce);
}
</style>
