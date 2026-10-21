<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { CircleAlert, Pencil, Plus } from "@lucide/vue";
import ChampDateComponent from "@/components/ChampDateComponent.vue";
import { vFocus } from "@/directives/focus.ts";
import { comptesService } from "@/services/comptesService.ts";
import { useNotificationStore } from "@/stores/notification";
import type { Periode } from "@/types/comptes.ts";
import { formatDateLongue, versIso } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";

const notification = useNotificationStore();

const periodes = ref<Periode[]>([]);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

const aujourdHui = versIso(new Date());

async function charger() {
    try {
        periodes.value = await comptesService.getPeriodes();
        errorMessage.value = null;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos périodes."
        );
    } finally {
        isLoading.value = false;
    }
}

onMounted(charger);

// Les plus récentes d'abord
const triees = computed(() =>
    [...periodes.value].sort((a, b) => b.dateDebut.localeCompare(a.dateDebut))
);

function enCoursAujourdhui(p: Periode): boolean {
    return p.dateDebut <= aujourdHui && aujourdHui <= p.dateFin;
}

// Même règle que le back : le mois de la date de fin (« Février 2026 »)
const moisAnnee = new Intl.DateTimeFormat("fr-FR", {
    month: "long",
    year: "numeric",
});
function nomParDefaut(dateFin: string): string {
    const [annee, mois, jour] = dateFin.split("-").map(Number);
    const nom = moisAnnee.format(new Date(annee, mois - 1, jour));
    return nom.charAt(0).toUpperCase() + nom.slice(1);
}

// ── Modification ──

const enEdition = ref<number | null>(null);
const nom = ref("");
const dateDebut = ref("");
const dateFin = ref("");
const erreurFormulaire = ref<string | null>(null);
const enregistrementEnCours = ref(false);

function ouvrirModification(p: Periode) {
    enEdition.value = p.id;
    // Nom par défaut laissé vide : il suivra la nouvelle date de fin
    nom.value = p.nom === nomParDefaut(p.dateFin) ? "" : p.nom;
    dateDebut.value = p.dateDebut;
    dateFin.value = p.dateFin;
    erreurFormulaire.value = null;
}

async function enregistrer() {
    if (enregistrementEnCours.value || enEdition.value === null) return;
    if (dateFin.value < dateDebut.value) {
        erreurFormulaire.value =
            "La date de fin doit être postérieure ou égale à la date de début.";
        return;
    }
    enregistrementEnCours.value = true;
    erreurFormulaire.value = null;
    try {
        await comptesService.modifierPeriode(enEdition.value, {
            nom: nom.value.trim() || null,
            dateDebut: dateDebut.value,
            dateFin: dateFin.value,
        });
        notification.success("Période modifiée.");
        enEdition.value = null;
        // Les périodes voisines ont pu bouger
        await charger();
    } catch (e: any) {
        erreurFormulaire.value = messageErreur(
            e,
            "Impossible de modifier la période."
        );
    } finally {
        enregistrementEnCours.value = false;
    }
}

// ── Ajout ──

const ajoutEnCours = ref(false);

async function ajouterSuivante() {
    ajoutEnCours.value = true;
    try {
        const p = await comptesService.ajouterPeriodeSuivante();
        notification.success(
            `Période « ${p.nom} » ajoutée, du ${formatDateLongue(p.dateDebut)} au ${formatDateLongue(p.dateFin)}.`
        );
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible d'ajouter la période.")
        );
    } finally {
        ajoutEnCours.value = false;
    }
}

// Première période (si l'utilisateur n'en a encore aucune) : le mois en cours
const premiereDebut = ref(versIso(new Date(new Date().setDate(1))));
const premiereFin = ref(
    versIso(new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0))
);

async function creerPremiere() {
    erreurFormulaire.value = null;
    if (premiereFin.value < premiereDebut.value) {
        erreurFormulaire.value =
            "La date de fin doit être postérieure ou égale à la date de début.";
        return;
    }
    ajoutEnCours.value = true;
    try {
        await comptesService.creerPeriode(
            premiereDebut.value,
            premiereFin.value
        );
        notification.success("Première période créée.");
        await charger();
    } catch (e: any) {
        erreurFormulaire.value = messageErreur(
            e,
            "Impossible de créer la période."
        );
    } finally {
        ajoutEnCours.value = false;
    }
}
</script>

<template>
    <p v-if="isLoading" class="message">Chargement...</p>
    <p v-else-if="errorMessage" class="message erreur">{{ errorMessage }}</p>

    <section v-else class="carte">
        <div class="en-tete-carte">
            <h2>Périodes</h2>
            <button
                v-if="periodes.length > 0"
                type="button"
                class="btn btn-secondaire"
                :disabled="ajoutEnCours"
                @click="ajouterSuivante"
            >
                <Plus aria-hidden="true" />
                Ajouter la période suivante
            </button>
        </div>
        <p class="aide">
            Les périodes se suivent sans trou : déplacer une date déplace aussi
            la limite de la période voisine. Une nouvelle période dure un mois ;
            ajustez sa fin à votre jour de paie. Les opérations suivent leur
            date et changent de période d'elles-mêmes.
        </p>

        <form
            v-if="periodes.length === 0"
            class="formulaire premiere"
            @submit.prevent="creerPremiere"
        >
            <ChampDateComponent
                id="premiere-debut"
                v-model="premiereDebut"
                label="Début"
            />
            <ChampDateComponent
                id="premiere-fin"
                v-model="premiereFin"
                label="Fin"
                :min="premiereDebut"
            />
            <button
                type="submit"
                class="btn btn-secondaire"
                :disabled="ajoutEnCours"
            >
                Créer la première période
            </button>
            <p v-if="erreurFormulaire" class="erreur" role="alert">
                <CircleAlert aria-hidden="true" />
                {{ erreurFormulaire }}
            </p>
        </form>

        <ul v-else class="liste">
            <li v-for="p in triees" :key="p.id">
                <form
                    v-if="enEdition === p.id"
                    class="formulaire edition"
                    @submit.prevent="enregistrer"
                >
                    <label class="champ large">
                        Nom
                        <input
                            v-focus
                            v-model="nom"
                            type="text"
                            maxlength="50"
                            :placeholder="`Par défaut : ${dateFin ? nomParDefaut(dateFin) : ''}`"
                        />
                    </label>
                    <ChampDateComponent
                        :id="`periode-debut-${p.id}`"
                        v-model="dateDebut"
                        label="Début"
                    />
                    <ChampDateComponent
                        :id="`periode-fin-${p.id}`"
                        v-model="dateFin"
                        label="Fin"
                        :min="dateDebut"
                    />
                    <div class="actions">
                        <button
                            type="submit"
                            class="btn btn-secondaire"
                            :disabled="enregistrementEnCours"
                        >
                            Enregistrer
                        </button>
                        <button
                            type="button"
                            class="btn btn-discret"
                            @click="enEdition = null"
                        >
                            Annuler
                        </button>
                    </div>
                    <p v-if="erreurFormulaire" class="erreur" role="alert">
                        <CircleAlert aria-hidden="true" />
                        {{ erreurFormulaire }}
                    </p>
                </form>

                <template v-else>
                    <div class="texte">
                        <span class="titre">
                            {{ p.nom }}
                            <span v-if="enCoursAujourdhui(p)" class="badge"
                                >En cours</span
                            >
                        </span>
                        <span class="details">
                            Du {{ formatDateLongue(p.dateDebut) }} au
                            {{ formatDateLongue(p.dateFin) }}
                        </span>
                    </div>
                    <div class="actions">
                        <button
                            type="button"
                            class="btn btn-icone"
                            title="Modifier"
                            aria-label="Modifier"
                            @click="ouvrirModification(p)"
                        >
                            <Pencil aria-hidden="true" />
                        </button>
                    </div>
                </template>
            </li>
        </ul>
    </section>
</template>

<style scoped>
.en-tete-carte {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 6px;
}

.en-tete-carte h2 {
    margin: 0;
}

.liste,
.premiere {
    margin-top: 12px !important;
}

.edition {
    flex: 1;
}

.erreur {
    flex-basis: 100%;
}
</style>
