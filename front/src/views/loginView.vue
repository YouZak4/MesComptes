<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/services/api";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";
import InputComponent from "@/components/InputComponent.vue";

const router = useRouter();
const auth = useAuthStore();
const notification = useNotificationStore();
const validityLogin = ref({
    identifiant: false,
    motDePasse: false,
});
const loginFormValid = computed(() =>
    Object.values(validityLogin.value).every(Boolean)
);
const identifiantLogin = ref("");
const motDePasseLogin = ref("");
// Le prénom, facultatif, n'a pas de validité à suivre (sa longueur est bornée par le champ)
const validityRegister = ref({
    nom: false,
    identifiant: false,
    motDePasse: false,
    email: false,
});
const nomRegister = ref("");
const prenomRegister = ref("");
const identifiantRegister = ref("");
const motDePasseRegister = ref("");
const confirmationRegister = ref("");
// Comparée ici et non via @valid : elle doit suivre aussi les changements du mot de passe
const confirmationValide = computed(
    () =>
        confirmationRegister.value !== "" &&
        confirmationRegister.value === motDePasseRegister.value
);
const registerFormValid = computed(
    () =>
        Object.values(validityRegister.value).every(Boolean) &&
        confirmationValide.value
);
const emailRegister = ref("");
const isLoading = ref(false);
const isRegister = ref(true);

function messageErreur(e: any, parDefaut: string): string {
    return e.response?.data?.message || parDefaut;
}

async function login() {
    isLoading.value = true;
    try {
        const { data } = await api.post("/auth/login", {
            identifiant: identifiantLogin.value,
            motDePasse: motDePasseLogin.value,
        });
        auth.setAuth(data);
        notification.success("Connexion réussie. Bienvenue !");
        await router.push("/");
    } catch {
        notification.error("Identifiant ou mot de passe incorrect.");
    } finally {
        isLoading.value = false;
    }
}

async function register() {
    isLoading.value = true;
    try {
        const { data } = await api.post("/auth/register", {
            nom: nomRegister.value.trim(),
            prenom: prenomRegister.value.trim() || null,
            identifiant: identifiantRegister.value,
            motDePasse: motDePasseRegister.value,
            email: emailRegister.value,
        });
        notification.success("Un code vous a été envoyé par e-mail.");
        await router.push({
            name: "verification",
            params: { jeton: data.jeton },
        });
    } catch (e: any) {
        notification.error(
            messageErreur(
                e,
                "Une erreur est survenue lors de la création du compte."
            )
        );
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
    <div class="main-container">
        <h1>Bienvenue sur : Mes comptes</h1>
        <h2>Votre outil de gestion de comptes</h2>

        <!-- ── Formulaire de connexion ── -->
        <div v-if="isRegister" class="login-container">
            <form @submit.prevent="login">
                <h2>Se connecter à Mes comptes</h2>

                <InputComponent
                    v-model="identifiantLogin"
                    label="Identifiant"
                    placeholder="Votre identifiant"
                    :required="true"
                    :min-length="8"
                    :max-length="30"
                    :regex="/^[a-zA-Z0-9_-]+$/"
                    regex-message="Entre 8 et 30 caractères, lettres, chiffres, _ et - uniquement."
                    @valid="validityLogin.identifiant = $event"
                />

                <InputComponent
                    v-model="motDePasseLogin"
                    type="password"
                    label="Mot de passe"
                    :required="true"
                    :min-length="8"
                    :regex="/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,30}$/"
                    regex-message="8 caractères minimum, avec au moins une majuscule, un chiffre et un caractère spécial."
                    @valid="validityLogin.motDePasse = $event"
                />

                <button
                    type="submit"
                    class="login-button"
                    :disabled="!loginFormValid"
                >
                    Connexion
                </button>
                <button
                    type="button"
                    class="login-button"
                    @click="isRegister = false"
                >
                    Créer un nouveau compte
                </button>
            </form>
        </div>

        <!-- ── Formulaire d'inscription ── -->
        <div v-if="!isRegister" class="register-container">
            <form @submit.prevent="register">
                <h2>Créez un compte pour utiliser Mes comptes</h2>

                <InputComponent
                    v-model="nomRegister"
                    label="Nom"
                    placeholder="Votre nom, ou un pseudo"
                    :required="true"
                    :max-length="100"
                    @valid="validityRegister.nom = $event"
                />

                <InputComponent
                    v-model="prenomRegister"
                    label="Prénom"
                    placeholder="Facultatif"
                    :max-length="100"
                />

                <InputComponent
                    v-model="identifiantRegister"
                    label="Identifiant"
                    placeholder="Votre identifiant de connexion"
                    :required="true"
                    :min-length="8"
                    :max-length="30"
                    :regex="/^[a-zA-Z0-9_-]+$/"
                    regex-message="Entre 8 et 30 caractères, lettres, chiffres, _ et - uniquement."
                    @valid="validityRegister.identifiant = $event"
                />

                <InputComponent
                    v-model="motDePasseRegister"
                    type="password"
                    label="Mot de passe"
                    :required="true"
                    :min-length="8"
                    :regex="/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,30}$/"
                    regex-message="8 caractères minimum, avec au moins une majuscule, un chiffre et un caractère spécial."
                    @valid="validityRegister.motDePasse = $event"
                />

                <InputComponent
                    v-model="confirmationRegister"
                    type="password"
                    label="Confirmation du mot de passe"
                    placeholder="Retapez votre mot de passe"
                    :required="true"
                    :correspond-a="motDePasseRegister"
                    correspond-a-message="Les deux mots de passe ne correspondent pas."
                />

                <InputComponent
                    v-model="emailRegister"
                    type="email"
                    label="Adresse e-mail"
                    placeholder="vous@exemple.com"
                    :required="true"
                    :regex="/^[^\s@]+@[^\s@]+\.[^\s@]+$/"
                    regex-message="Adresse e-mail invalide."
                    @valid="validityRegister.email = $event"
                />

                <button
                    type="submit"
                    class="register-button"
                    :disabled="!registerFormValid"
                >
                    Créer mon compte
                </button>
                <button
                    type="button"
                    class="register-button"
                    @click="isRegister = true"
                >
                    J'ai déjà un compte
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

    .login-button,
    .register-button {
        display: block;
        margin: 0.5rem auto;
    }
}
</style>
