<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { CircleAlert } from "@lucide/vue";
import { vFocus } from "@/directives/focus.ts";
import { utilisateurService } from "@/services/utilisateurService.ts";
import { useNotificationStore } from "@/stores/notification";
import { useProfilStore } from "@/stores/profil";
import type { ChangementEmail } from "@/types/utilisateur.ts";
import { messageErreur } from "@/utils/erreurs.ts";

const notification = useNotificationStore();
const profil = useProfilStore();

const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

const nom = ref("");
const prenom = ref("");
const erreurNom = ref<string | null>(null);
const nomEnCours = ref(false);

onMounted(async () => {
    try {
        const [moi, changement] = await Promise.all([
            profil.utilisateur ?? profil.charger(),
            utilisateurService.getChangementEmail(),
        ]);
        nom.value = moi.nom;
        prenom.value = moi.prenom ?? "";
        definirChangement(changement);
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger votre profil."
        );
    } finally {
        isLoading.value = false;
    }
});

// Rien à enregistrer tant que le nom et le prénom n'ont pas changé
const nomInchange = computed(
    () =>
        nom.value.trim() === profil.utilisateur?.nom &&
        prenom.value.trim() === (profil.utilisateur?.prenom ?? "")
);

async function enregistrerNom() {
    if (nomEnCours.value) return;
    nomEnCours.value = true;
    erreurNom.value = null;
    try {
        const modifie = await utilisateurService.modifierProfil(
            nom.value.trim(),
            prenom.value.trim()
        );
        profil.mettreAJour(modifie);
        nom.value = modifie.nom;
        prenom.value = modifie.prenom ?? "";
        notification.success("Nom modifié.");
    } catch (e: any) {
        erreurNom.value = messageErreur(e, "Impossible de modifier le nom.");
    } finally {
        nomEnCours.value = false;
    }
}

// ── Adresse mail : demande, puis code reçu sur la nouvelle adresse ──

const changement = ref<ChangementEmail | null>(null);
const formulaireEmail = ref(false);
const nouvelEmail = ref("");
const motDePasseEmail = ref("");
const code = ref("");
const erreurEmail = ref<string | null>(null);
const emailEnCours = ref(false);

// Décompte avant de pouvoir redemander un code
const secondesAvantRenvoi = ref(0);
let minuterie: ReturnType<typeof setInterval> | undefined;

function definirChangement(c: ChangementEmail | null) {
    changement.value = c;
    code.value = "";
    clearInterval(minuterie);
    secondesAvantRenvoi.value = c?.secondesAvantRenvoi ?? 0;
    if (secondesAvantRenvoi.value > 0) {
        minuterie = setInterval(() => {
            secondesAvantRenvoi.value--;
            if (secondesAvantRenvoi.value <= 0) clearInterval(minuterie);
        }, 1000);
    }
}

onUnmounted(() => clearInterval(minuterie));

function ouvrirFormulaireEmail() {
    formulaireEmail.value = true;
    nouvelEmail.value = "";
    motDePasseEmail.value = "";
    erreurEmail.value = null;
}

async function demanderChangement() {
    if (emailEnCours.value) return;
    emailEnCours.value = true;
    erreurEmail.value = null;
    try {
        const c = await utilisateurService.demanderChangementEmail(
            nouvelEmail.value.trim(),
            motDePasseEmail.value
        );
        motDePasseEmail.value = "";
        formulaireEmail.value = false;
        definirChangement(c);
    } catch (e: any) {
        erreurEmail.value = messageErreur(
            e,
            "Impossible de demander le changement d'adresse."
        );
    } finally {
        emailEnCours.value = false;
    }
}

async function verifierCode() {
    if (emailEnCours.value) return;
    emailEnCours.value = true;
    erreurEmail.value = null;
    try {
        const modifie = await utilisateurService.verifierChangementEmail(
            code.value.trim()
        );
        profil.mettreAJour(modifie);
        definirChangement(null);
        notification.success("Adresse mail changée.");
    } catch (e: any) {
        erreurEmail.value = messageErreur(e, "Impossible de vérifier le code.");
        code.value = "";
        // Code expiré ou trop de tentatives : le changement n'existe plus côté serveur
        try {
            const c = await utilisateurService.getChangementEmail();
            if (c === null) definirChangement(null);
        } catch {
            // L'état affiché reste celui d'avant
        }
    } finally {
        emailEnCours.value = false;
    }
}

async function renvoyerCode() {
    if (emailEnCours.value) return;
    emailEnCours.value = true;
    erreurEmail.value = null;
    try {
        definirChangement(
            await utilisateurService.renvoyerCodeChangementEmail()
        );
        notification.success("Un nouveau code a été envoyé.");
    } catch (e: any) {
        erreurEmail.value = messageErreur(e, "Impossible de renvoyer le code.");
    } finally {
        emailEnCours.value = false;
    }
}

async function annulerChangement() {
    if (emailEnCours.value) return;
    emailEnCours.value = true;
    erreurEmail.value = null;
    try {
        await utilisateurService.annulerChangementEmail();
        definirChangement(null);
        notification.success("Changement d'adresse annulé.");
    } catch (e: any) {
        erreurEmail.value = messageErreur(
            e,
            "Impossible d'annuler le changement."
        );
    } finally {
        emailEnCours.value = false;
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
            <form class="formulaire" @submit.prevent="enregistrerNom">
                <label class="champ large">
                    Nom
                    <input
                        v-model="nom"
                        type="text"
                        maxlength="100"
                        autocomplete="family-name"
                        required
                    />
                </label>
                <label class="champ large">
                    Prénom (facultatif)
                    <input
                        v-model="prenom"
                        type="text"
                        maxlength="100"
                        autocomplete="given-name"
                    />
                </label>
                <button
                    type="submit"
                    class="btn btn-secondaire"
                    :disabled="nomEnCours || nomInchange"
                >
                    Enregistrer
                </button>
                <p v-if="erreurNom" class="erreur" role="alert">
                    <CircleAlert aria-hidden="true" />
                    {{ erreurNom }}
                </p>
            </form>
            <p class="aide">
                Sans prénom, le nom seul est affiché : il peut servir de pseudo.
            </p>
        </section>

        <section class="carte">
            <h2>Adresse mail</h2>
            <p class="email">
                Adresse actuelle :
                <strong>{{ profil.utilisateur?.email }}</strong>
            </p>

            <!-- Code envoyé : on attend sa saisie -->
            <template v-if="changement">
                <p class="aide">
                    Un code a été envoyé à
                    <strong>{{ changement.nouvelEmail }}</strong
                    >. Saisissez-le pour confirmer : l'adresse actuelle reste
                    valable d'ici là.
                </p>
                <form class="formulaire" @submit.prevent="verifierCode">
                    <label class="champ">
                        Code reçu
                        <input
                            v-focus
                            v-model="code"
                            type="text"
                            inputmode="numeric"
                            autocomplete="one-time-code"
                            maxlength="6"
                            pattern="\d{6}"
                            title="Le code à 6 chiffres reçu par mail"
                            class="code"
                            required
                        />
                    </label>
                    <div class="actions">
                        <button
                            type="submit"
                            class="btn btn-secondaire"
                            :disabled="emailEnCours"
                        >
                            Confirmer
                        </button>
                        <button
                            type="button"
                            class="btn btn-secondaire"
                            :disabled="emailEnCours || secondesAvantRenvoi > 0"
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
                            :disabled="emailEnCours"
                            @click="annulerChangement"
                        >
                            Annuler le changement
                        </button>
                    </div>
                </form>
            </template>

            <form
                v-else-if="formulaireEmail"
                class="formulaire"
                @submit.prevent="demanderChangement"
            >
                <label class="champ large">
                    Nouvelle adresse
                    <input
                        v-focus
                        v-model="nouvelEmail"
                        type="email"
                        autocomplete="email"
                        required
                    />
                </label>
                <label class="champ large">
                    Mot de passe
                    <input
                        v-model="motDePasseEmail"
                        type="password"
                        autocomplete="current-password"
                        required
                    />
                </label>
                <div class="actions">
                    <button
                        type="submit"
                        class="btn btn-secondaire"
                        :disabled="emailEnCours"
                    >
                        Envoyer le code
                    </button>
                    <button
                        type="button"
                        class="btn btn-discret"
                        @click="formulaireEmail = false"
                    >
                        Annuler
                    </button>
                </div>
            </form>

            <button
                v-else
                type="button"
                class="btn btn-secondaire"
                @click="ouvrirFormulaireEmail"
            >
                Changer d'adresse mail
            </button>

            <p v-if="erreurEmail" class="erreur message-email" role="alert">
                <CircleAlert aria-hidden="true" />
                {{ erreurEmail }}
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
                    class="btn btn-secondaire"
                    :disabled="motDePasseEnCours"
                >
                    Changer le mot de passe
                </button>
                <p v-if="erreurMotDePasse" class="erreur" role="alert">
                    <CircleAlert aria-hidden="true" />
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
    margin: 0 0 12px;
}

.formulaire + .aide,
.aide + .formulaire {
    margin-top: 12px;
}

.code {
    width: 8rem;
    letter-spacing: 0.2em;
}

.message-email {
    margin-top: 12px;
}
</style>
