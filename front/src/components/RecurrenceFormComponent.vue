<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import ChampLibelleComponent from "@/components/ChampLibelleComponent.vue";
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
        <div class="fond" @mousedown.self="emit('fermer')">
            <div
                class="fenetre"
                role="dialog"
                aria-modal="true"
                aria-labelledby="titre-recurrence"
            >
                <h2 id="titre-recurrence">
                    {{
                        enModification
                            ? "Modifier la récurrence"
                            : "Nouvelle récurrence"
                    }}
                </h2>

                <form @submit.prevent="enregistrer">
                    <label class="champ">
                        Montant
                        <span class="montant">
                            <input
                                ref="champMontant"
                                v-model="montantTexte"
                                type="text"
                                inputmode="decimal"
                                autocomplete="off"
                                placeholder="0,00"
                                required
                            />
                            <span class="devise">€</span>
                        </span>
                    </label>

                    <ChampLibelleComponent
                        id="recurrence-libelle"
                        v-model:texte="libelleTexte"
                        v-model:libelle="libelleExistant"
                    />

                    <div class="ligne">
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
                                        {{ c.icone }} {{ c.nom }}
                                    </option>
                                </optgroup>
                                <optgroup label="Revenus">
                                    <option
                                        v-for="c in categoriesRevenus"
                                        :key="c.id"
                                        :value="c.id"
                                    >
                                        {{ c.icone }} {{ c.nom }}
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

                    <fieldset class="calendrier">
                        <legend>Calendrier</legend>

                        <div class="ligne">
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

                        <div class="ligne">
                            <label class="champ">
                                À partir du
                                <input
                                    v-model="dateDebut"
                                    type="date"
                                    required
                                />
                            </label>

                            <label class="champ">
                                Jusqu'au
                                <input
                                    v-model="dateFin"
                                    type="date"
                                    :min="dateDebut"
                                />
                            </label>
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
                        {{ erreur }}
                    </p>

                    <div class="actions">
                        <button
                            type="button"
                            class="bouton-secondaire"
                            @click="emit('fermer')"
                        >
                            Annuler
                        </button>
                        <button
                            type="submit"
                            class="bouton-principal"
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
.fond {
    position: fixed;
    inset: 0;
    z-index: 950;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: 6vh 1rem 1rem;
    background: rgba(30, 20, 45, 0.45);
    overflow-y: auto;
}

.fenetre {
    width: min(100%, 520px);
    padding: 1.5rem;
    border-radius: 12px;
    background: #ffffff;
    color: #45335c;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
}

h2 {
    margin: 0 0 1rem;
    font-size: 1.25rem;
}

form {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.champ {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    min-width: 0;
}

.ligne {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.75rem;
}

.calendrier {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
    margin: 0;
    padding: 0.75rem;
    border: 1px solid #e3d3f1;
    border-radius: 10px;
}

legend {
    padding: 0 0.35rem;
    font-weight: bold;
}

input,
select {
    padding: 0.55rem 0.75rem;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    color: #000000;
    font-size: 0.95rem;
    font-family: inherit;
    min-width: 0;
}

input:focus,
select:focus {
    outline: 2px solid #45335c;
    outline-offset: 1px;
}

.montant {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.montant input {
    flex: 1;
    font-size: 1.5rem;
    font-weight: bold;
}

.devise {
    font-size: 1.5rem;
    font-weight: bold;
}

.resume {
    margin: 0;
    font-weight: bold;
}

.aide {
    margin: 0;
    font-size: 0.85rem;
    color: #81758e;
}

.erreur {
    margin: 0;
    color: #c62727;
}

.actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 0.5rem;
    margin-top: 0.25rem;
}

.bouton-principal,
.bouton-secondaire {
    padding: 0.6rem 1rem;
    border-radius: 10px;
    font-size: 0.95rem;
    cursor: pointer;
}

.bouton-principal {
    border: none;
    background: #45335c;
    color: #ffffff;
}

.bouton-secondaire {
    border: 1px solid #45335c;
    background: transparent;
    color: #45335c;
}

.bouton-principal:disabled {
    opacity: 0.6;
    cursor: default;
}
</style>
