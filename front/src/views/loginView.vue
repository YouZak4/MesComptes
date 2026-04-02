<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/services/api"; // ← ton instance axios centralisée
import { useAuthStore } from "@/stores/auth"; // ← le store

const router = useRouter();
const auth = useAuthStore();

const identifiantLogin = ref("");
const motDePasseLogin = ref("");
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
        <div v-if="isRegister" class="login-container">
            <form @submit.prevent="login">
                <h2>Se connecter à Demon invasion</h2>
                <div class="login-input">
                    <input
                        v-model="identifiantLogin"
                        type="text"
                        placeholder="Identifiant"
                        required
                    />
                </div>
                <div class="login-input">
                    <input
                        v-model="motDePasseLogin"
                        type="password"
                        placeholder="Mot de passe"
                        required
                    />
                </div>

                <p v-if="erreur" class="erreur">{{ erreur }}</p>

                <button
                    type="submit"
                    class="login-button"
                    :disabled="isLoading"
                >
                    {{ isLoading ? "Connexion..." : "Se connecter" }}
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
        <div v-if="!isRegister" class="register-container">
            <form @submit.prevent="register">
                <h2>Créez un compte pour jouer à Demon invasion</h2>
                <div class="register-input">
                    <input
                        v-model="pseudoRegister"
                        type="text"
                        placeholder="Pseudonyme"
                        required
                    />
                </div>
                <div class="register-input">
                    <input
                        v-model="identifiantRegister"
                        type="text"
                        placeholder="Identifiant"
                        required
                    />
                </div>
                <div class="register-input">
                    <input
                        v-model="motDePasseRegister"
                        type="password"
                        placeholder="Mot de passe"
                        required
                    />
                </div>
                <div class="register-input">
                    <input
                        v-model="emailRegister"
                        type="text"
                        placeholder="E-mail"
                        required
                    />
                </div>

                <p v-if="erreur" class="erreur">{{ erreur }}</p>

                <button
                    type="submit"
                    class="register-button"
                    :disabled="isLoading"
                >
                    {{ isLoading ? "Connexion..." : "Créer mon compte" }}
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
    width: 50%;
    margin: 15% auto auto;
    display: flex;
    flex-direction: column;
    text-align: center;
    border: red 1px solid;
    .login-container {
        display: flex;
        flex-direction: column;
        border: 1px solid blue;
        .login-input {
            margin: 1rem auto;
        }
        .login-button {
            display: block;
            margin: 1rem auto;
        }
    }
    .register-container {
        display: flex;
        flex-direction: column;
        border: 1px solid blue;
        .register-input {
            margin: 1rem auto;
        }
        .register-button {
            display: block;
            margin: 1rem auto;
        }
    }
}
</style>
