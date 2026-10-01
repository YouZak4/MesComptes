<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/services/api";
import { useNotificationStore } from "@/stores/notification";
import InputComponent from "@/components/InputComponent.vue";

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
        notification.error(
            messageErreur(e, "Impossible de renvoyer le code.")
        );
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
        // 409 : identifiant/pseudo pris entre-temps, il faut recommencer l'inscription
        if (e.response?.status === 409) {
            await router.replace({ name: "login" });
        }
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
    <div class="main-container">
        <h1>Demon invasion</h1>

        <div class="register-container">
            <form @submit.prevent="verifier">
                <h2>Vérifiez votre adresse e-mail</h2>
                <p>Saisissez le code à 6 chiffres envoyé à {{ emailMasque }}</p>

                <InputComponent
                    v-model="codeVerification"
                    label="Code de vérification"
                    :required="true"
                    :regex="/^\d{6}$/"
                    regex-message="Le code contient 6 chiffres."
                    @valid="codeValide = $event"
                />

                <button
                    type="submit"
                    class="register-button"
                    :disabled="!codeValide || isLoading"
                >
                    Valider
                </button>
                <button
                    type="button"
                    class="register-button"
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
                    class="register-button"
                    @click="router.replace({ name: 'login' })"
                >
                    Retour à la connexion
                </button>
            </form>
        </div>
    </div>
</template>

<style scoped>
.main-container {
    font-family: Arial, Helvetica, sans-serif;
    font-size: 1rem;
    width: 25%;
    margin: 10% auto auto;
    display: flex;
    flex-direction: column;
    text-align: center;

    .register-container {
        display: flex;
        flex-direction: column;
        padding: 1.5rem;

        form {
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
        }
    }

    .register-button {
        display: block;
        margin: 0.5rem auto;
    }
}
</style>
