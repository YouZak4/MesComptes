<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import {
    Archive,
    ChevronDown,
    ChevronRight,
    CircleAlert,
    Pencil,
    Plus,
} from "@lucide/vue";
import ChoixApparenceComponent from "@/components/reglages/ChoixApparenceComponent.vue";
import PastilleCategorieComponent from "@/components/PastilleCategorieComponent.vue";
import { vFocus } from "@/directives/focus.ts";
import { comptesService } from "@/services/comptesService.ts";
import { useNotificationStore } from "@/stores/notification";
import type {
    Categorie,
    Libelle,
    TypeMasquable,
    TypeOperation,
} from "@/types/comptes.ts";
import { COULEURS_DEPENSES, ICONES } from "@/utils/categories.ts";
import { messageErreur } from "@/utils/erreurs.ts";

const notification = useNotificationStore();

const categories = ref<Categorie[]>([]);
const libelles = ref<Libelle[]>([]);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

async function charger() {
    try {
        const [listeCategories, listeLibelles] = await Promise.all([
            comptesService.getCategories(),
            comptesService.getLibelles(),
        ]);
        categories.value = listeCategories;
        libelles.value = listeLibelles;
        errorMessage.value = null;
    } catch (e: any) {
        errorMessage.value = messageErreur(
            e,
            "Impossible de charger vos catégories."
        );
    } finally {
        isLoading.value = false;
    }
}

onMounted(charger);

// Masqué (élément fourni) ou archivé (élément personnel) : plus proposé à la saisie
function inactive(c: Categorie): boolean {
    return c.masquee || c.archive;
}
function inactif(l: Libelle): boolean {
    return l.masque || l.archive;
}

// ── Recherche et dépliage ──

const recherche = ref("");
const ouvertes = ref(new Set<number>());

function normaliser(texte: string): string {
    return texte.normalize("NFD").replace(/[̀-ͯ]/g, "").trim().toLowerCase();
}

function libellesDe(categorieId: number): Libelle[] {
    const filtre = normaliser(recherche.value);
    return libelles.value.filter(
        (l) =>
            l.categorieId === categorieId &&
            (filtre === "" ||
                normaliser(l.nom).includes(filtre) ||
                normaliser(nomCategorie(categorieId)).includes(filtre))
    );
}

function nomCategorie(id: number): string {
    return categories.value.find((c) => c.id === id)?.nom ?? "";
}

// Une catégorie apparaît si son nom ou l'un de ses libellés correspond à la recherche
const visibles = computed(() => {
    const filtre = normaliser(recherche.value);
    if (filtre === "") return categories.value;
    return categories.value.filter(
        (c) =>
            normaliser(c.nom).includes(filtre) ||
            libelles.value.some(
                (l) =>
                    l.categorieId === c.id && normaliser(l.nom).includes(filtre)
            )
    );
});

const groupes = computed(() =>
    (
        [
            { type: "DEPENSE", titre: "Dépenses" },
            { type: "REVENU", titre: "Revenus" },
        ] as const
    ).map((g) => ({
        ...g,
        categories: visibles.value.filter((c) => c.type === g.type),
    }))
);

// Pendant une recherche, tout est déplié pour montrer les libellés trouvés
function estOuverte(id: number): boolean {
    return recherche.value.trim() !== "" || ouvertes.value.has(id);
}

function basculer(id: number) {
    const copie = new Set(ouvertes.value);
    if (copie.has(id)) copie.delete(id);
    else copie.add(id);
    ouvertes.value = copie;
}

// ── Masquage des éléments fournis ──

const enCours = ref<string | null>(null);

async function basculerMasquage(
    type: TypeMasquable,
    id: number,
    nom: string,
    masque: boolean
) {
    enCours.value = `${type}-${id}`;
    try {
        if (masque) {
            await comptesService.afficher(type, id);
            notification.success(`« ${nom} » est de nouveau proposé.`);
        } else {
            await comptesService.masquer(type, id);
            notification.success(
                `« ${nom} » est masqué : il n'est plus proposé à la saisie.`
            );
        }
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de changer l'affichage.")
        );
    } finally {
        enCours.value = null;
    }
}

// ── Édition : une seule à la fois (nouvelle catégorie, catégorie ou libellé) ──

const edition = ref<string | null>(null);
const erreurFormulaire = ref<string | null>(null);
const enregistrementEnCours = ref(false);

const nomCat = ref("");
const typeCat = ref<TypeOperation>("DEPENSE");
// Nouvelle catégorie : première icône et premier couple de la charte
const iconeCat = ref("package");
const couleurCat = ref(COULEURS_DEPENSES[0].couleur);

function ouvrirNouvelleCategorie() {
    edition.value = "nouvelle-categorie";
    nomCat.value = "";
    typeCat.value = "DEPENSE";
    iconeCat.value = "package";
    couleurCat.value = COULEURS_DEPENSES[0].couleur;
    erreurFormulaire.value = null;
}

function ouvrirCategorie(c: Categorie) {
    edition.value = `categorie-${c.id}`;
    nomCat.value = c.nom;
    typeCat.value = c.type;
    iconeCat.value = c.icone && ICONES[c.icone] ? c.icone : "package";
    couleurCat.value = c.couleur ?? COULEURS_DEPENSES[0].couleur;
    erreurFormulaire.value = null;
}

async function enregistrerCategorie(existante: Categorie | null) {
    if (enregistrementEnCours.value) return;
    enregistrementEnCours.value = true;
    erreurFormulaire.value = null;
    try {
        const valeurs = {
            nom: nomCat.value.trim(),
            type: typeCat.value,
            icone: iconeCat.value,
            couleur: couleurCat.value,
            archive: existante?.archive ?? false,
        };
        if (existante) {
            await comptesService.modifierCategorie(existante.id, valeurs);
            notification.success("Catégorie modifiée.");
        } else {
            const creee = await comptesService.creerCategorie(valeurs);
            notification.success(`Catégorie « ${creee.nom} » créée.`);
            ouvertes.value = new Set([...ouvertes.value, creee.id]);
        }
        edition.value = null;
        await charger();
    } catch (e: any) {
        erreurFormulaire.value = messageErreur(
            e,
            "Impossible d'enregistrer la catégorie."
        );
    } finally {
        enregistrementEnCours.value = false;
    }
}

const nomLib = ref("");
const categorieLib = ref<number | null>(null);

// Catégories proposées pour un libellé : celles en usage, plus sa catégorie actuelle
function categoriesPour(l: Libelle | null): Categorie[] {
    return categories.value.filter(
        (c) => !inactive(c) || c.id === l?.categorieId
    );
}

function ouvrirLibelle(l: Libelle) {
    edition.value = `libelle-${l.id}`;
    nomLib.value = l.nom;
    categorieLib.value = l.categorieId;
    erreurFormulaire.value = null;
}

function ouvrirNouveauLibelle(categorieId: number) {
    edition.value = `nouveau-libelle-${categorieId}`;
    nomLib.value = "";
    categorieLib.value = categorieId;
    erreurFormulaire.value = null;
}

async function enregistrerLibelle(existant: Libelle | null) {
    if (enregistrementEnCours.value || categorieLib.value === null) return;
    enregistrementEnCours.value = true;
    erreurFormulaire.value = null;
    try {
        if (existant) {
            await comptesService.modifierLibelle(existant.id, {
                nom: nomLib.value.trim(),
                categorieId: categorieLib.value,
                archive: existant.archive,
            });
            notification.success("Libellé modifié.");
        } else {
            const cree = await comptesService.creerLibelle(
                nomLib.value.trim(),
                categorieLib.value
            );
            notification.success(`Libellé « ${cree.nom} » créé.`);
        }
        edition.value = null;
        await charger();
    } catch (e: any) {
        erreurFormulaire.value = messageErreur(
            e,
            "Impossible d'enregistrer le libellé."
        );
    } finally {
        enregistrementEnCours.value = false;
    }
}

// ── Retrait (archivage ou suppression) et désarchivage des éléments personnels ──

const aRetirer = ref<string | null>(null);

async function retirerCategorie(c: Categorie) {
    enCours.value = `categorie-${c.id}`;
    try {
        const archivee = await comptesService.supprimerCategorie(c.id);
        notification.success(
            archivee
                ? `« ${c.nom} » est archivée : elle n'est plus proposée, l'historique la garde.`
                : `« ${c.nom} » est supprimée (elle n'était pas utilisée).`
        );
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de retirer la catégorie.")
        );
    } finally {
        enCours.value = null;
        aRetirer.value = null;
    }
}

async function retirerLibelle(l: Libelle) {
    enCours.value = `libelle-${l.id}`;
    try {
        const archive = await comptesService.supprimerLibelle(l.id);
        notification.success(
            archive
                ? `« ${l.nom} » est archivé : il n'est plus proposé, l'historique le garde.`
                : `« ${l.nom} » est supprimé (il n'était pas utilisé).`
        );
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de retirer le libellé.")
        );
    } finally {
        enCours.value = null;
        aRetirer.value = null;
    }
}

async function desarchiverCategorie(c: Categorie) {
    enCours.value = `categorie-${c.id}`;
    try {
        await comptesService.modifierCategorie(c.id, {
            nom: c.nom,
            type: c.type,
            icone: c.icone,
            couleur: c.couleur,
            archive: false,
        });
        notification.success(`« ${c.nom} » est de nouveau proposée.`);
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de désarchiver la catégorie.")
        );
    } finally {
        enCours.value = null;
    }
}

async function desarchiverLibelle(l: Libelle) {
    enCours.value = `libelle-${l.id}`;
    try {
        await comptesService.modifierLibelle(l.id, {
            nom: l.nom,
            categorieId: l.categorieId,
            archive: false,
        });
        notification.success(`« ${l.nom} » est de nouveau proposé.`);
        await charger();
    } catch (e: any) {
        notification.error(
            messageErreur(e, "Impossible de désarchiver le libellé.")
        );
    } finally {
        enCours.value = null;
    }
}
</script>

<template>
    <p v-if="isLoading" class="message">Chargement...</p>
    <p v-else-if="errorMessage" class="message erreur">{{ errorMessage }}</p>

    <template v-else>
        <section class="carte">
            <div class="barre">
                <input
                    v-model="recherche"
                    type="search"
                    class="recherche"
                    placeholder="Rechercher une catégorie ou un libellé"
                    aria-label="Rechercher une catégorie ou un libellé"
                />
                <button
                    v-if="edition !== 'nouvelle-categorie'"
                    type="button"
                    class="btn btn-secondaire"
                    @click="ouvrirNouvelleCategorie"
                >
                    <Plus aria-hidden="true" />
                    Nouvelle catégorie
                </button>
            </div>
            <p class="aide">
                Les éléments fournis sont communs à tous : vous pouvez les
                masquer. Les vôtres se modifient ; retirés, ils sont archivés
                s'ils ont déjà servi, pour que l'historique les garde.
            </p>

            <form
                v-if="edition === 'nouvelle-categorie'"
                class="formulaire nouvelle"
                @submit.prevent="enregistrerCategorie(null)"
            >
                <label class="champ large">
                    Nom
                    <input
                        v-focus
                        v-model="nomCat"
                        type="text"
                        maxlength="50"
                        required
                    />
                </label>
                <label class="champ">
                    Type
                    <select v-model="typeCat">
                        <option value="DEPENSE">Dépense</option>
                        <option value="REVENU">Revenu</option>
                    </select>
                </label>
                <ChoixApparenceComponent
                    v-model:icone="iconeCat"
                    v-model:couleur="couleurCat"
                    :type="typeCat"
                    nom-categorie="nouvelle"
                />
                <div class="actions">
                    <button
                        type="submit"
                        class="btn btn-secondaire"
                        :disabled="enregistrementEnCours"
                    >
                        Créer
                    </button>
                    <button
                        type="button"
                        class="btn btn-discret"
                        @click="edition = null"
                    >
                        Annuler
                    </button>
                </div>
                <p v-if="erreurFormulaire" class="erreur" role="alert">
                    <CircleAlert aria-hidden="true" />
                    {{ erreurFormulaire }}
                </p>
            </form>
        </section>

        <section v-for="groupe in groupes" :key="groupe.type" class="carte">
            <h2>{{ groupe.titre }}</h2>
            <p v-if="groupe.categories.length === 0" class="vide">
                Aucune catégorie ne correspond.
            </p>

            <ul class="liste">
                <li
                    v-for="c in groupe.categories"
                    :key="c.id"
                    class="categorie"
                    :class="{ inactif: inactive(c) }"
                >
                    <form
                        v-if="edition === `categorie-${c.id}`"
                        class="formulaire edition"
                        @submit.prevent="enregistrerCategorie(c)"
                    >
                        <label class="champ large">
                            Nom
                            <input
                                v-focus
                                v-model="nomCat"
                                type="text"
                                maxlength="50"
                                required
                            />
                        </label>
                        <label class="champ">
                            Type
                            <select v-model="typeCat">
                                <option value="DEPENSE">Dépense</option>
                                <option value="REVENU">Revenu</option>
                            </select>
                        </label>
                        <ChoixApparenceComponent
                            v-model:icone="iconeCat"
                            v-model:couleur="couleurCat"
                            :type="typeCat"
                            :nom-categorie="String(c.id)"
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
                                @click="edition = null"
                            >
                                Annuler
                            </button>
                        </div>
                        <p v-if="erreurFormulaire" class="erreur" role="alert">
                            <CircleAlert aria-hidden="true" />
                            {{ erreurFormulaire }}
                        </p>
                    </form>

                    <div v-else class="ligne">
                        <button
                            type="button"
                            class="deplier"
                            :aria-expanded="estOuverte(c.id)"
                            @click="basculer(c.id)"
                        >
                            <span class="fleche" aria-hidden="true">
                                <ChevronDown
                                    v-if="estOuverte(c.id)"
                                    :size="18"
                                />
                                <ChevronRight v-else :size="18" />
                            </span>
                            <PastilleCategorieComponent :categorie="c" />
                            <span class="texte-categorie">
                                <span class="titre">
                                    {{ c.nom }}
                                    <span v-if="!c.fournie" class="badge"
                                        >Personnelle</span
                                    >
                                    <span v-if="c.masquee" class="badge"
                                        >Masquée</span
                                    >
                                    <span v-if="c.archive" class="badge"
                                        >Archivée</span
                                    >
                                </span>
                                <span class="nombre"
                                    >{{
                                        libellesDe(c.id).length
                                    }}
                                    libellé(s)</span
                                >
                            </span>
                        </button>

                        <div
                            v-if="aRetirer === `categorie-${c.id}`"
                            class="actions confirmation"
                        >
                            <span>Retirer cette catégorie ?</span>
                            <button
                                type="button"
                                class="btn btn-danger"
                                :disabled="enCours === `categorie-${c.id}`"
                                @click="retirerCategorie(c)"
                            >
                                Retirer
                            </button>
                            <button
                                type="button"
                                class="btn btn-discret"
                                @click="aRetirer = null"
                            >
                                Annuler
                            </button>
                        </div>
                        <div v-else-if="c.fournie" class="actions">
                            <button
                                type="button"
                                class="btn btn-discret"
                                :disabled="enCours === `categorie-${c.id}`"
                                @click="
                                    basculerMasquage(
                                        'categorie',
                                        c.id,
                                        c.nom,
                                        c.masquee
                                    )
                                "
                            >
                                {{ c.masquee ? "Afficher" : "Masquer" }}
                            </button>
                        </div>
                        <div v-else class="actions">
                            <button
                                type="button"
                                class="btn btn-icone"
                                title="Modifier"
                                aria-label="Modifier"
                                @click="ouvrirCategorie(c)"
                            >
                                <Pencil aria-hidden="true" />
                            </button>
                            <button
                                v-if="c.archive"
                                type="button"
                                class="btn btn-discret"
                                :disabled="enCours === `categorie-${c.id}`"
                                @click="desarchiverCategorie(c)"
                            >
                                Désarchiver
                            </button>
                            <button
                                v-else
                                type="button"
                                class="btn btn-icone"
                                title="Archiver (ou supprimer si elle n'a jamais servi)"
                                aria-label="Retirer"
                                @click="aRetirer = `categorie-${c.id}`"
                            >
                                <Archive aria-hidden="true" />
                            </button>
                        </div>
                    </div>

                    <!-- Libellés de la catégorie -->
                    <ul v-if="estOuverte(c.id)" class="libelles">
                        <li
                            v-for="l in libellesDe(c.id)"
                            :key="l.id"
                            :class="{ inactif: inactif(l) }"
                        >
                            <form
                                v-if="edition === `libelle-${l.id}`"
                                class="formulaire edition"
                                @submit.prevent="enregistrerLibelle(l)"
                            >
                                <label class="champ large">
                                    Nom
                                    <input
                                        v-focus
                                        v-model="nomLib"
                                        type="text"
                                        maxlength="100"
                                        required
                                    />
                                </label>
                                <label class="champ large">
                                    Catégorie par défaut
                                    <select v-model="categorieLib">
                                        <option
                                            v-for="option in categoriesPour(l)"
                                            :key="option.id"
                                            :value="option.id"
                                        >
                                            {{ option.nom }}
                                        </option>
                                    </select>
                                </label>
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
                                        @click="edition = null"
                                    >
                                        Annuler
                                    </button>
                                </div>
                                <p
                                    v-if="erreurFormulaire"
                                    class="erreur"
                                    role="alert"
                                >
                                    {{ erreurFormulaire }}
                                </p>
                            </form>

                            <template v-else>
                                <span class="titre-libelle">
                                    {{ l.nom }}
                                    <span v-if="!l.fourni" class="badge"
                                        >Personnel</span
                                    >
                                    <span v-if="l.masque" class="badge"
                                        >Masqué</span
                                    >
                                    <span v-if="l.archive" class="badge"
                                        >Archivé</span
                                    >
                                </span>

                                <div
                                    v-if="aRetirer === `libelle-${l.id}`"
                                    class="actions confirmation"
                                >
                                    <span>Retirer ce libellé ?</span>
                                    <button
                                        type="button"
                                        class="btn btn-danger"
                                        :disabled="
                                            enCours === `libelle-${l.id}`
                                        "
                                        @click="retirerLibelle(l)"
                                    >
                                        Retirer
                                    </button>
                                    <button
                                        type="button"
                                        class="btn btn-discret"
                                        @click="aRetirer = null"
                                    >
                                        Annuler
                                    </button>
                                </div>
                                <div v-else-if="l.fourni" class="actions">
                                    <button
                                        type="button"
                                        class="btn btn-discret"
                                        :disabled="
                                            enCours === `libelle-${l.id}`
                                        "
                                        @click="
                                            basculerMasquage(
                                                'libelle',
                                                l.id,
                                                l.nom,
                                                l.masque
                                            )
                                        "
                                    >
                                        {{ l.masque ? "Afficher" : "Masquer" }}
                                    </button>
                                </div>
                                <div v-else class="actions">
                                    <button
                                        type="button"
                                        class="btn btn-icone"
                                        title="Modifier"
                                        aria-label="Modifier"
                                        @click="ouvrirLibelle(l)"
                                    >
                                        <Pencil aria-hidden="true" />
                                    </button>
                                    <button
                                        v-if="l.archive"
                                        type="button"
                                        class="btn btn-discret"
                                        :disabled="
                                            enCours === `libelle-${l.id}`
                                        "
                                        @click="desarchiverLibelle(l)"
                                    >
                                        Désarchiver
                                    </button>
                                    <button
                                        v-else
                                        type="button"
                                        class="btn btn-icone"
                                        title="Archiver (ou supprimer s'il n'a jamais servi)"
                                        aria-label="Retirer"
                                        @click="aRetirer = `libelle-${l.id}`"
                                    >
                                        <Archive aria-hidden="true" />
                                    </button>
                                </div>
                            </template>
                        </li>

                        <li v-if="libellesDe(c.id).length === 0" class="vide">
                            Aucun libellé.
                        </li>

                        <!-- Ajout d'un libellé personnel dans cette catégorie -->
                        <li v-if="!inactive(c)" class="ajout-libelle">
                            <form
                                v-if="edition === `nouveau-libelle-${c.id}`"
                                class="formulaire"
                                @submit.prevent="enregistrerLibelle(null)"
                            >
                                <input
                                    v-focus
                                    v-model="nomLib"
                                    type="text"
                                    maxlength="100"
                                    placeholder="Nom du libellé"
                                    aria-label="Nom du nouveau libellé"
                                    required
                                />
                                <button
                                    type="submit"
                                    class="btn btn-secondaire"
                                    :disabled="enregistrementEnCours"
                                >
                                    Ajouter
                                </button>
                                <button
                                    type="button"
                                    class="btn btn-discret"
                                    @click="edition = null"
                                >
                                    Annuler
                                </button>
                                <p
                                    v-if="erreurFormulaire"
                                    class="erreur"
                                    role="alert"
                                >
                                    {{ erreurFormulaire }}
                                </p>
                            </form>
                            <button
                                v-else
                                type="button"
                                class="btn btn-discret"
                                @click="ouvrirNouveauLibelle(c.id)"
                            >
                                <Plus aria-hidden="true" />
                                Ajouter un libellé
                            </button>
                        </li>
                    </ul>
                </li>
            </ul>
        </section>
    </template>
</template>

<style scoped>
.barre {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 8px;
}

.recherche {
    flex: 1 1 240px;
}

.nouvelle {
    margin-top: 16px;
}

.erreur {
    flex-basis: 100%;
}

.categorie {
    flex-direction: column;
    align-items: stretch !important;
}

.ligne {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
}

/* Actions à droite, y compris quand elles passent à la ligne */
.ligne > .actions {
    margin-left: auto;
}

.edition {
    flex: 1;
}

.deplier {
    flex: 1 1 180px;
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    min-height: 44px;
    padding: 0;
    border: none;
    background: none;
    color: inherit;
    font: inherit;
    text-align: left;
    cursor: pointer;
}

.deplier:focus-visible {
    border-radius: var(--rayon-m);
}

.fleche {
    display: flex;
    flex: none;
    color: var(--encre-douce);
}

.texte-categorie {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.nombre {
    font-size: 12px;
    color: var(--encre-douce);
}

/* Libellés : décalés sous leur catégorie, sans trait de séparation */
.libelles {
    list-style: none;
    margin: 4px 0 4px 66px;
    padding: 0;
}

.libelles > li {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    min-height: 44px;
    padding: 2px 0;
}

.libelles > li.vide {
    align-items: flex-start;
    min-height: 0;
    padding: 4px 0;
    text-align: left;
}

.titre-libelle {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 2px 8px;
}

.inactif .titre-libelle {
    opacity: 0.55;
}

.ajout-libelle .formulaire {
    align-items: center;
}

@media (max-width: 640px) {
    .libelles {
        margin-left: 24px;
    }
}
</style>
