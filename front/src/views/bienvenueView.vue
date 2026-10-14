<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import EnTeteComponent from "@/components/EnTeteComponent.vue";
import ReglagesComptesComponent from "@/components/reglages/ReglagesComptesComponent.vue";
import RecurrencesCourantesComponent from "@/components/assistant/RecurrencesCourantesComponent.vue";
import { comptesService } from "@/services/comptesService.ts";
import { useNotificationStore } from "@/stores/notification";
import type { Periode } from "@/types/comptes.ts";
import { versIso } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import "@/styles/reglages.css";

const route = useRoute();
const router = useRouter();
const notification = useNotificationStore();

const etapes = [
    { numero: 1, titre: "Période" },
    { numero: 2, titre: "Comptes" },
    { numero: 3, titre: "Récurrences" },
] as const;

// Période de référence des récurrences ; null tant qu'aucune n'existe
const periode = ref<Periode | null>(null);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

// Étape dans l'URL (un rechargement la garde) ; sans période, on reste à la première
const etape = computed<number>(() => {
    if (periode.value === null) return 1;
    const demandee = Number(route.query.etape);
    return demandee === 3 ? 3 : 2;
});

function allerA(numero: number) {
    router.replace({ query: { etape: String(numero) } });
}

onMounted(async () => {
    try {
        const periodes = await comptesService.getPeriodes();
        const aujourdHui = versIso(new Date());
        periode.value =
            periodes.find(
                (p) => p.dateDebut <= aujourdHui && aujourdHui <= p.dateFin
            ) ??
            periodes[0] ??
            null;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos périodes."
        );
    } finally {
        isLoading.value = false;
    }
});

// ── Étape 1 : première période, préremplie avec le mois en cours ──

const maintenant = new Date();
const debut = ref(
    versIso(new Date(maintenant.getFullYear(), maintenant.getMonth(), 1))
);
const fin = ref(
    versIso(new Date(maintenant.getFullYear(), maintenant.getMonth() + 1, 0))
);
const erreurPeriode = ref<string | null>(null);
const creationEnCours = ref(false);

async function creerPeriode() {
    if (creationEnCours.value) return;
    erreurPeriode.value = null;
    creationEnCours.value = true;
    try {
        periode.value = await comptesService.creerPeriode(
            debut.value,
            fin.value
        );
        allerA(2);
    } catch (e: any) {
        erreurPeriode.value = messageErreur(
            e,
            "Impossible de créer la période."
        );
    } finally {
        creationEnCours.value = false;
    }
}

// ── Fin ──

function terminer(nbRecurrences: number) {
    notification.success(
        nbRecurrences > 0
            ? `Tout est prêt : ${nbRecurrences} récurrence(s) créée(s).`
            : "Tout est prêt. Appuyez sur N pour saisir une opération."
    );
    router.push({ name: "accueil" });
}
</script>

<template>
    <EnTeteComponent />

    <main class="reglages assistant">
        <h1>Bienvenue sur Mes comptes</h1>

        <ol class="etapes">
            <li
                v-for="e in etapes"
                :key="e.numero"
                :class="{ active: etape === e.numero, faite: etape > e.numero }"
                :aria-current="etape === e.numero ? 'step' : undefined"
            >
                <span class="numero">{{
                    etape > e.numero ? "✓" : e.numero
                }}</span>
                {{ e.titre }}
            </li>
        </ol>

        <p v-if="isLoading" class="message">Chargement...</p>
        <p v-else-if="errorMessage" class="message erreur">
            {{ errorMessage }}
        </p>

        <!-- Étape 1 : période -->
        <section v-else-if="etape === 1" class="carte">
            <h2>Votre première période</h2>
            <p class="aide">
                Une période est votre « mois de compte », par exemple d'un jour
                de paie au suivant : les totaux du tableau de bord sont calculés
                sur elle. Les suivantes seront ajoutées d'elles-mêmes, d'un mois
                chacune ; vous pourrez ajuster leurs dates dans les réglages.
            </p>
            <form class="formulaire periode" @submit.prevent="creerPeriode">
                <label class="champ">
                    Début
                    <input v-model="debut" type="date" required />
                </label>
                <label class="champ">
                    Fin
                    <input v-model="fin" type="date" :min="debut" required />
                </label>
                <button
                    type="submit"
                    class="bouton-principal"
                    :disabled="creationEnCours"
                >
                    Continuer
                </button>
                <p v-if="erreurPeriode" class="erreur" role="alert">
                    {{ erreurPeriode }}
                </p>
            </form>
        </section>

        <!-- Étape 2 : comptes -->
        <template v-else-if="etape === 2">
            <p class="aide">
                Un premier compte a été créé à votre inscription : renommez-le
                et donnez son solde actuel avec ✏️, puis ajoutez vos autres
                comptes (livret, compte joint…).
            </p>
            <ReglagesComptesComponent />
            <div class="actions navigation">
                <button
                    type="button"
                    class="bouton-principal"
                    @click="allerA(3)"
                >
                    Continuer
                </button>
            </div>
        </template>

        <!-- Étape 3 : récurrences -->
        <template v-else-if="periode">
            <RecurrencesCourantesComponent
                :periode="periode"
                @terminee="terminer"
            />
            <div class="actions navigation">
                <button
                    type="button"
                    class="bouton-secondaire"
                    @click="allerA(2)"
                >
                    ← Comptes
                </button>
            </div>
        </template>

        <p v-if="!isLoading && periode" class="aide plus-tard">
            Vous pouvez quitter l'assistant à tout moment : tout ce qui est
            proposé ici se retrouve dans « Réglages » et « Récurrences ».
            <RouterLink :to="{ name: 'accueil' }"
                >Aller au tableau de bord</RouterLink
            >
        </p>
    </main>
</template>

<style scoped>
.etapes {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1.5rem;
    margin: 0;
    padding: 0;
    list-style: none;
    color: #81758e;
}

.etapes li {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.etapes .numero {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1.8rem;
    height: 1.8rem;
    border: 2px solid #e3d3f1;
    border-radius: 50%;
    font-weight: bold;
}

.etapes .active {
    color: #45335c;
    font-weight: bold;
}

.etapes .active .numero {
    border-color: #45335c;
    background: #45335c;
    color: #ffffff;
}

.etapes .faite .numero {
    border-color: #14a81a;
    color: #14a81a;
}

.periode {
    margin-top: 0.75rem;
}

.periode .erreur {
    flex-basis: 100%;
}

.navigation {
    justify-content: flex-end;
    gap: 0.5rem !important;
}

.plus-tard a {
    color: #45335c;
    font-weight: bold;
}
</style>
