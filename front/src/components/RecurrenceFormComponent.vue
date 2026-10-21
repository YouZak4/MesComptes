<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { CircleAlert } from "@lucide/vue";
import ChampDateComponent from "@/components/ChampDateComponent.vue";
import ChampLibelleComponent from "@/components/ChampLibelleComponent.vue";
import LibellesDeCategorieComponent from "@/components/LibellesDeCategorieComponent.vue";
import { comptesService } from "@/services/comptesService.ts";
import type {
    Categorie,
    Compte,
    Frequence,
    Libelle,
    Recurrence,
} from "@/types/comptes.ts";
import { formatDateLongue, versIso } from "@/utils/format.ts";
import { messageErreur } from "@/utils/erreurs.ts";
import { decrireCalendrier, jourDe } from "@/utils/recurrences.ts";

// Création (recurrence null) ou modification d'une récurrence, dans une fenêtre
const props = defineProps<{
    recurrence: Recurrence | null;
    libelle: Libelle | null;
}>();
const emit = defineEmits<{
    fermer: [];
    enregistree: [recurrence: Recurrence, creation: boolean];
}>();

const comptes = ref<Compte[]>([]);
const categories = ref<Categorie[]>([]);
const chargementReferentiels = ref(false);
const enregistrementEnCours = ref(false);
const erreur = ref<string | null>(null);

const aujourdHui = versIso(new Date());

const montantTexte = ref("");
const libelleTexte = ref("");
const libelleExistant = ref<Libelle | null>(null);
const categorieId = ref<number | null>(null);
const compteId = ref<number | null>(null);
const frequence = ref<Frequence>("MENSUELLE");
const dateDebut = ref(aujourdHui);
const jour = ref(jourDe(aujourdHui));
// Jour choisi à la main (31 pour « fin de mois ») : changer la date de début ne l'écrase plus
const jourModifie = ref(false);
const dateFin = ref("");
const note = ref("");

const champMontant = ref<HTMLInputElement | null>(null);

const enModification = computed(() => props.recurrence !== null);

onMounted(async () => {
    window.addEventListener("keydown", surTouche);
    if (props.recurrence) await preremplir(props.recurrence);
    await nextTick();
    champMontant.value?.focus();
    await chargerReferentiels();
});
onUnmounted(() => window.removeEventListener("keydown", surTouche));

// Échap ferme la fenêtre (dans le libellé, il ferme d'abord les suggestions)
function surTouche(event: KeyboardEvent) {
    if (event.key === "Escape") emit("fermer");
}

async function preremplir(r: Recurrence) {
    montantTexte.value = String(r.montant).replace(".", ",");
    if (props.libelle) {
        libelleExistant.value = props.libelle;
        libelleTexte.value = props.libelle.nom;
    }
    compteId.value = r.compteId;
    frequence.value = r.frequence;
    dateDebut.value = r.dateDebut;
    jour.value = r.jour;
    jourModifie.value = r.jour !== jourDe(r.dateDebut);
    dateFin.value = r.dateFin ?? "";
    note.value = r.note ?? "";
    // Après la mise à jour qui suit le choix du libellé, qui propose sa catégorie par défaut
    await nextTick();
    categorieId.value = r.categorieId;
}

async function chargerReferentiels() {
    chargementReferentiels.value = true;
    try {
        const [listeComptes, listeCategories] = await Promise.all([
            comptesService.getComptes(),
            comptesService.getCategories(),
        ]);
        // Un compte ou une catégorie archivé(e) ou masqué(e) reste proposé(e) s'il est déjà celui de la récurrence
        const actuelle = props.recurrence;
        comptes.value = listeComptes.filter(
            (c) => !c.archive || c.id === actuelle?.compteId
        );
        categories.value = listeCategories.filter(
            (c) => (!c.masquee && !c.archive) || c.id === actuelle?.categorieId
        );
        if (
            compteId.value === null ||
            !comptes.value.some((c) => c.id === compteId.value)
        ) {
            const parDefaut =
                comptes.value.find((c) => c.dernierUtilise) ?? comptes.value[0];
            compteId.value = parDefaut?.id ?? null;
        }
    } catch (e: any) {
        erreur.value = messageErreur(e, "Impossible de charger vos comptes.");
    } finally {
        chargementReferentiels.value = false;
    }
}

const categoriesDepenses = computed(() =>
    categories.value.filter((c) => c.type === "DEPENSE")
);
const categoriesRevenus = computed(() =>
    categories.value.filter((c) => c.type === "REVENU")
);

const nouveauLibelle = computed(
    () => libelleTexte.value.trim() !== "" && libelleExistant.value === null
);

// Libellé reconnu : sa catégorie est proposée (elle reste modifiable ensuite)
watch(libelleExistant, (libelle) => {
    if (libelle) categorieId.value = libelle.categorieId;
});

// Libellé choisi d'un clic parmi ceux de la catégorie
function choisirLibelle(libelle: Libelle) {
    libelleExistant.value = libelle;
    libelleTexte.value = libelle.nom;
}

watch(dateDebut, (date) => {
    if (!jourModifie.value && date) jour.value = jourDe(date);
});

const jourValide = computed(
    () => Number.isInteger(jour.value) && jour.value >= 1 && jour.value <= 31
);

// « Le 5 de chaque mois, jusqu'au 31 décembre 2027. »
const resume = computed(() => {
    if (!jourValide.value || !dateDebut.value) return "";
    const calendrier = decrireCalendrier(
        frequence.value,
        jour.value,
        dateDebut.value
    );
    const fin = dateFin.value
        ? `jusqu'au ${formatDateLongue(dateFin.value)}`
        : "sans date de fin";
    return `${calendrier}, ${fin}.`;
});

// À la création, les occurrences passées depuis la date de début sont créées tout de suite
const rattrapage = computed(
    () => !enModification.value && dateDebut.value <= aujourdHui
);

// Pour reprendre une récurrence arrêtée, il suffit de retirer ou repousser sa date de fin
const arretee = computed(
    () => props.recurrence !== null && !props.recurrence.active
);

function lireMontant(texte: string): number | null {
    const nettoye = texte.replace(/\s/g, "").replace(",", ".");
    if (!/^\d+(\.\d{1,2})?$/.test(nettoye)) return null;
    const montant = Number(nettoye);
    return montant > 0 ? montant : null;
}

async function enregistrer() {
    if (enregistrementEnCours.value) return;
    erreur.value = null;

    const montant = lireMontant(montantTexte.value);
    if (montant === null) {
        erreur.value =
            "Le montant doit être un nombre positif, avec au plus deux décimales.";
        champMontant.value?.focus();
        return;
    }
    if (compteId.value === null) {
        erreur.value = "Choisissez un compte.";
        return;
    }
    if (categorieId.value === null) {
        erreur.value = nouveauLibelle.value
            ? `Choisissez la catégorie du nouveau libellé « ${libelleTexte.value.trim()} ».`
            : "Choisissez un libellé ou une catégorie.";
        return;
    }
    if (!jourValide.value) {
        erreur.value = "Le jour doit être compris entre 1 et 31.";
        return;
    }
    if (dateFin.value && dateFin.value < dateDebut.value) {
        erreur.value =
            "La date de fin doit être postérieure ou égale à la date de début.";
        return;
    }

    enregistrementEnCours.value = true;
    try {
        let libelleId = libelleExistant.value?.id ?? null;
        if (nouveauLibelle.value) {
            const cree = await comptesService.creerLibelle(
                libelleTexte.value.trim(),
                categorieId.value
            );
            libelleId = cree.id;
            // Le libellé existe maintenant : une nouvelle tentative ne le recréera pas
            libelleExistant.value = cree;
        }
        const valeurs = {
            compteId: compteId.value,
            montant,
            categorieId: categorieId.value,
            libelleId,
            note: note.value.trim() || null,
            frequence: frequence.value,
            jour: jour.value,
            dateDebut: dateDebut.value,
            dateFin: dateFin.value || null,
        };
        const enregistree = props.recurrence
            ? await comptesService.modifierRecurrence(
                  props.recurrence.id,
                  valeurs
              )
            : await comptesService.creerRecurrence(valeurs);
        emit("enregistree", enregistree, !props.recurrence);
    } catch (e: any) {
        erreur.value = messageErreur(
            e,
            "Impossible d'enregistrer la récurrence."
        );
    } finally {
        enregistrementEnCours.value = false;
    }
}
</script>

<template>
    <Teleport to="body">
        <div class="voile" @mousedown.self="emit('fermer')">
            <div
                class="fenetre"
                role="dialog"
                aria-modal="true"
                aria-labelledby="titre-recurrence"
            >
                <h2 id="titre-recurrence" class="titre-fenetre">
                    {{
                        enModification
                            ? "Modifier la récurrence"
                            : "Nouvelle récurrence"
                    }}
                </h2>

                <form @submit.prevent="enregistrer">
                    <label class="champ">
                        Montant
                        <span class="champ-montant">
                            <input
                                ref="champMontant"
                                v-model="montantTexte"
                                type="text"
                                inputmode="decimal"
                                autocomplete="off"
                                placeholder="0,00"
                                required
                            />
                            <span class="devise" aria-hidden="true">€</span>
                        </span>
                    </label>

                    <ChampLibelleComponent
                        id="recurrence-libelle"
                        v-model:texte="libelleTexte"
                        v-model:libelle="libelleExistant"
                    />

                    <div class="deux-colonnes">
                        <label class="champ">
                            Catégorie
                            <select v-model="categorieId" required>
                                <option :value="null" disabled>
                                    {{
                                        chargementReferentiels
                                            ? "Chargement..."
                                            : "Choisir une catégorie"
                                    }}
                                </option>
                                <optgroup label="Dépenses">
                                    <option
                                        v-for="c in categoriesDepenses"
                                        :key="c.id"
                                        :value="c.id"
                                    >
                                        {{ c.nom }}
                                    </option>
                                </optgroup>
                                <optgroup label="Revenus">
                                    <option
                                        v-for="c in categoriesRevenus"
                                        :key="c.id"
                                        :value="c.id"
                                    >
                                        {{ c.nom }}
                                    </option>
                                </optgroup>
                            </select>
                        </label>

                        <label class="champ">
                            Compte
                            <select v-model="compteId" required>
                                <option
                                    v-if="comptes.length === 0"
                                    :value="null"
                                    disabled
                                >
                                    {{
                                        chargementReferentiels
                                            ? "Chargement..."
                                            : "Aucun compte"
                                    }}
                                </option>
                                <option
                                    v-for="c in comptes"
                                    :key="c.id"
                                    :value="c.id"
                                >
                                    {{ c.nom }}
                                </option>
                            </select>
                        </label>
                    </div>

                    <LibellesDeCategorieComponent
                        v-if="libelleExistant === null"
                        :categorie-id="categorieId"
                        @choisir="choisirLibelle"
                    />

                    <fieldset class="calendrier">
                        <legend>Calendrier</legend>

                        <div class="deux-colonnes">
                            <label class="champ">
                                Fréquence
                                <select v-model="frequence">
                                    <option value="MENSUELLE">
                                        Chaque mois
                                    </option>
                                    <option value="ANNUELLE">
                                        Chaque année
                                    </option>
                                </select>
                            </label>

                            <label class="champ">
                                Jour du mois
                                <input
                                    v-model.number="jour"
                                    type="number"
                                    min="1"
                                    max="31"
                                    required
                                    @input="jourModifie = true"
                                />
                            </label>
                        </div>

                        <div class="deux-colonnes">
                            <ChampDateComponent
                                id="recurrence-debut"
                                v-model="dateDebut"
                                label="À partir du"
                            />

                            <ChampDateComponent
                                id="recurrence-fin"
                                v-model="dateFin"
                                label="Jusqu'au"
                                :min="dateDebut"
                                facultatif
                            />
                        </div>

                        <p v-if="resume" class="resume">{{ resume }}</p>
                        <p class="aide">
                            Un jour absent du mois (31 en avril) devient le
                            dernier jour du mois.
                            <template v-if="frequence === 'ANNUELLE'">
                                Le mois est celui de la date de début.
                            </template>
                        </p>
                        <p v-if="rattrapage" class="aide">
                            Les opérations déjà passées depuis cette date seront
                            créées tout de suite.
                        </p>
                        <p v-if="enModification" class="aide">
                            Seules les opérations à venir changent : celles déjà
                            créées restent telles quelles.
                        </p>
                        <p v-if="arretee" class="aide">
                            Récurrence arrêtée : retirez ou repoussez la date de
                            fin pour la reprendre à partir de demain.
                        </p>
                    </fieldset>

                    <label class="champ">
                        Note
                        <input
                            v-model="note"
                            type="text"
                            maxlength="255"
                            placeholder="Facultatif"
                        />
                    </label>

                    <p v-if="erreur" class="erreur" role="alert">
                        <CircleAlert aria-hidden="true" />
                        {{ erreur }}
                    </p>

                    <div class="pied-fenetre">
                        <button
                            type="button"
                            class="btn btn-discret"
                            @click="emit('fermer')"
                        >
                            Annuler
                        </button>
                        <button
                            type="submit"
                            class="btn btn-principal"
                            :disabled="enregistrementEnCours"
                        >
                            Enregistrer
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.calendrier {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin: 0;
    padding: 14px;
    border: 0;
    border-radius: var(--rayon-l);
    background: var(--vert-50);
}

/* Légende posée en haut de l'encart (et non sur sa bordure) */
legend {
    float: left;
    width: 100%;
    padding: 0;
    font-size: 13px;
    font-weight: 800;
}

legend + * {
    clear: both;
}

.resume {
    font-weight: 700;
}
</style>
