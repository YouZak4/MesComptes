<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/services/api";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";
import { TrendingUp } from "@lucide/vue";
import InputComponent from "@/components/InputComponent.vue";
import "@/styles/connexion.css";

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

            <!-- ── Formulaire de connexion ── -->
            <form v-if="isRegister" @submit.prevent="login">
                <h2 class="titre-formulaire">Se connecter</h2>

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

                <div class="boutons">
                    <button
                        type="submit"
                        class="btn btn-principal"
                        :disabled="!loginFormValid"
                    >
                        Connexion
                    </button>
                    <button
                        type="button"
                        class="btn btn-discret"
                        @click="isRegister = false"
                    >
                        Créer un nouveau compte
                    </button>
                </div>
            </form>

            <!-- ── Formulaire d'inscription ── -->
            <form v-else @submit.prevent="register">
                <h2 class="titre-formulaire">Créer un compte</h2>

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

                <div class="boutons">
                    <button
                        type="submit"
                        class="btn btn-principal"
                        :disabled="!registerFormValid"
                    >
                        Créer mon compte
                    </button>
                    <button
                        type="button"
                        class="btn btn-discret"
                        @click="isRegister = true"
                    >
                        J'ai déjà un compte
                    </button>
                </div>
            </form>
        </div>
    </main>
</template>
