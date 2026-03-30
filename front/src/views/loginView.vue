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

async function login() {
    erreur.value = null;
    isLoading.value = true;
    try {
        const { data } = await api.post("/auth/login", {
            identifiant: identifiantLogin.value,
            motDePasse: motDePasseLogin.value,
        });

        auth.setAuth(data); // ← remplace les 3 localStorage.setItem
        await router.push("/"); // ← chemin correct vers l'accueil
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

        auth.setAuth(data); // ← idem
        await router.push("/");
    } catch (e: any) {
        erreur.value = e.response?.data || "Erreur de connexion.";
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
    <div class="login-container">
        <form @submit.prevent="login">
            <h1>Vous avez déjà un compte ?</h1>
            <h2>Connectez-vous !</h2>
            <div>
                <label>Identifiant</label>
                <input
                    v-model="identifiantLogin"
                    type="text"
                    placeholder="Votre identifiant"
                    required
                />
            </div>

            <div>
                <label>Mot de passe</label>
                <input
                    v-model="motDePasseLogin"
                    type="password"
                    placeholder="Votre mot de passe"
                    required
                />
            </div>

            <p v-if="erreur" class="erreur">{{ erreur }}</p>

            <button type="submit" :disabled="isLoading">
                {{ isLoading ? "Connexion..." : "Se connecter" }}
            </button>
        </form>
        <form @submit.prevent="register">
            <h1>Pas encore de compte ?</h1>
            <h2>Renseignez vos informations pour en créer un !</h2>
            <div>
                <label>Pseudonyme</label>
                <input
                    v-model="pseudoRegister"
                    type="text"
                    placeholder="Votre pseudonyme"
                    required
                />
            </div>
            <div>
                <label>Identifiant</label>
                <input
                    v-model="identifiantRegister"
                    type="text"
                    placeholder="Votre identifiant"
                    required
                />
            </div>
            <div>
                <label>Mot de passe</label>
                <input
                    v-model="motDePasseRegister"
                    type="password"
                    placeholder="Votre mot de passe"
                    required
                />
            </div>
            <div>
                <label>E-mail</label>
                <input
                    v-model="emailRegister"
                    type="text"
                    placeholder="Votre email"
                    required
                />
            </div>

            <p v-if="erreur" class="erreur">{{ erreur }}</p>

            <button type="submit" :disabled="isLoading">
                {{ isLoading ? "Connexion..." : "Créer mon compte" }}
            </button>
        </form>
    </div>
</template>
