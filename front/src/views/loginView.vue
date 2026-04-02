<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/services/api";
import { useAuthStore } from "@/stores/auth";

// On importe notre composant
import InputComponent from "@/components/InputComponent.vue";

const router = useRouter();
const auth = useAuthStore();

const validityLogin = ref({
    identifiant: false,
    motDePasse: false,
});
const loginFormValid = computed(() =>
    Object.values(validityLogin.value).every(Boolean)
);
const identifiantLogin = ref("");
const motDePasseLogin = ref("");
const validityRegister = ref({
    pseudo: false,
    identifiant: false,
    motDePasse: false,
    email: false,
});
const registerFormValid = computed(() =>
    Object.values(validityRegister.value).every(Boolean)
);
const pseudoRegister = ref("");
const identifiantRegister = ref("");
const motDePasseRegister = ref("");
const emailRegister = ref("");
const erreur = ref<string | null>(null);
const isLoading = ref(false);
const isRegister = ref(true);

async function login() {
    erreur.value = null;
    isLoading.value = true;
    try {
        const { data } = await api.post("/auth/login", {
            identifiant: identifiantLogin.value,
            motDePasse: motDePasseLogin.value,
        });
        auth.setAuth(data);
        await router.push("/");
    } catch (e: any) {
        erreur.value = e.response?.data || "Erreur de connexion.";
    } finally {
        isLoading.value = false;
    }
}

async function register() {
    erreur.value = null;
    isLoading.value = true;
    try {
        const { data } = await api.post("/auth/register", {
            pseudo: pseudoRegister.value,
            identifiant: identifiantRegister.value,
            motDePasse: motDePasseRegister.value,
            email: emailRegister.value,
        });
        console.log(data);
        isRegister.value = true;
    } catch (e: any) {
        erreur.value = e.response?.data || "Erreur de connexion.";
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
    <div class="main-container">
        <h1>Demon invasion</h1>

        <!-- ── Formulaire de connexion ── -->
        <div v-if="isRegister" class="login-container">
            <form @submit.prevent="login">
                <h2>Se connecter à Demon invasion</h2>

                <InputComponent
                    v-model="identifiantLogin"
                    label="Identifiant"
                    placeholder="Votre identifiant"
                    :required="true"
                    :min-length="8"
                    :max-length="30"
                    :regex="/^[a-zA-Z0-9_-]+$/"
                    regex-message="Lettres, chiffres, _ et - uniquement."
                    @valid="validityLogin.identifiant = $event"
                />

                <InputComponent
                    v-model="motDePasseLogin"
                    type="password"
                    label="Mot de passe"
                    :required="true"
                    :min-length="8"
                    :max-length="30"
                    :regex="/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,30}$/"
                    regex-message="8-30 caractères, avec au moins une majuscule, un chiffre et un caractère spécial."
                    @valid="validityLogin.motDePasse = $event"
                />

                <p v-if="erreur" class="erreur">
                    Identifiant ou mot de passe incorrect
                </p>

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
                <h2>Créez un compte pour jouer à Demon invasion</h2>

                <InputComponent
                    v-model="pseudoRegister"
                    label="Pseudonyme"
                    placeholder="Votre pseudo en jeu"
                    :required="true"
                    :min-length="2"
                    :max-length="20"
                    :regex="/^[a-zA-Z0-9_-]+$/"
                    regex-message="Lettres, chiffres, _ et - uniquement."
                    @valid="validityRegister.pseudo = $event"
                />

                <InputComponent
                    v-model="identifiantRegister"
                    label="Identifiant"
                    placeholder="Votre identifiant de connexion"
                    :required="true"
                    :min-length="3"
                    :max-length="30"
                    :regex="/^[a-zA-Z0-9_-]+$/"
                    regex-message="Lettres, chiffres, _ et - uniquement."
                    @valid="validityRegister.identifiant = $event"
                />

                <InputComponent
                    v-model="motDePasseRegister"
                    type="password"
                    label="Mot de passe"
                    :required="true"
                    :regex="/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,30}$/"
                    regex-message="8-30 caractères, avec au moins une majuscule, un chiffre et un caractère spécial."
                    @valid="validityRegister.motDePasse = $event"
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

    .login-container {
        .erreur {
            margin-top: 0.5rem;
            color: #c62727;
            text-align: left;
        }
    }
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
