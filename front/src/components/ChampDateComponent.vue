<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from "vue";
import { CalendarDays, ChevronLeft, ChevronRight } from "@lucide/vue";
import type { Periode } from "@/types/comptes.ts";
import { versDate, versIso } from "@/utils/format.ts";

// Champ date avec un calendrier aux couleurs de l'appli, à la place de celui du navigateur.
// Avec des périodes, le calendrier montre une période entière (du 25 juillet au 24 août…)
// et les flèches passent d'une période à l'autre ; hors des périodes connues, un mois civil
const props = withDefaults(
    defineProps<{
        id: string;
        label: string;
        periodes?: Periode[];
        min?: string;
        // Date facultative : peut rester vide, et être effacée
        facultatif?: boolean;
    }>(),
    { periodes: () => [], min: "", facultatif: false }
);
const date = defineModel<string>({ required: true });

const JOURS_SEMAINE = ["L", "M", "M", "J", "V", "S", "D"];

const formatChamp = new Intl.DateTimeFormat("fr-FR", {
    weekday: "short",
    day: "numeric",
    month: "long",
    year: "numeric",
});
const formatMois = new Intl.DateTimeFormat("fr-FR", {
    month: "long",
    year: "numeric",
});
const formatMoisCourt = new Intl.DateTimeFormat("fr-FR", { month: "short" });
const formatJourMois = new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "short",
});
const formatJourComplet = new Intl.DateTimeFormat("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
});

const champ = ref<HTMLElement | null>(null);
const declencheur = ref<HTMLButtonElement | null>(null);
const grille = ref<HTMLElement | null>(null);
const ouvert = ref(false);
// Le calendrier s'aligne à droite du champ quand il dépasserait de l'écran,
// et s'ouvre au-dessus quand la place manque en dessous
const aDroite = ref(false);
const auDessus = ref(false);
const HAUTEUR_CALENDRIER = 420;

interface Fenetre {
    debut: string;
    fin: string;
    titre: string;
}
const fenetre = ref<Fenetre | null>(null);
// Jour qui a le focus clavier dans la grille
const jourActif = ref("");

function aujourdhui(): string {
    return versIso(new Date());
}

function ajouterJours(iso: string, nb: number): string {
    const d = versDate(iso);
    return versIso(new Date(d.getFullYear(), d.getMonth(), d.getDate() + nb));
}

// Lundi = 0 … dimanche = 6
function jourSemaine(iso: string): number {
    return (versDate(iso).getDay() + 6) % 7;
}

// La période qui contient le jour, sinon son mois civil
function fenetreContenant(iso: string): Fenetre {
    const periode = props.periodes.find(
        (p) => p.dateDebut <= iso && iso <= p.dateFin
    );
    if (periode) {
        return {
            debut: periode.dateDebut,
            fin: periode.dateFin,
            titre: periode.nom,
        };
    }
    const d = versDate(iso);
    const titre = formatMois.format(d);
    return {
        debut: versIso(new Date(d.getFullYear(), d.getMonth(), 1)),
        fin: versIso(new Date(d.getFullYear(), d.getMonth() + 1, 0)),
        titre: titre.charAt(0).toUpperCase() + titre.slice(1),
    };
}

const sousTitre = computed(() =>
    fenetre.value
        ? `du ${formatJourMois.format(versDate(fenetre.value.debut))} au ${formatJourMois.format(versDate(fenetre.value.fin))}`
        : ""
);

// Semaines entières, du lundi avant le début au dimanche après la fin
const jours = computed(() => {
    if (!fenetre.value) return [];
    const { debut, fin } = fenetre.value;
    const premier = ajouterJours(debut, -jourSemaine(debut));
    const dernier = ajouterJours(fin, 6 - jourSemaine(fin));
    const liste = [];
    for (let iso = premier; iso <= dernier; iso = ajouterJours(iso, 1)) {
        const d = versDate(iso);
        const dansFenetre = debut <= iso && iso <= fin;
        liste.push({
            iso,
            numero: d.getDate(),
            dansFenetre,
            // Repère du mois au début de la période et à chaque 1er : la période est à cheval sur deux mois
            mois:
                dansFenetre && (iso === debut || d.getDate() === 1)
                    ? formatMoisCourt.format(d).replace(".", "")
                    : null,
            desactive: props.min !== "" && iso < props.min,
            libelle: formatJourComplet.format(d),
        });
    }
    return liste;
});

const texteChamp = computed(() =>
    date.value ? formatChamp.format(versDate(date.value)) : "Aucune date"
);

// ── Ouverture et fermeture ──

function ouvrir() {
    const depart =
        date.value || (props.min > aujourdhui() ? props.min : aujourdhui());
    fenetre.value = fenetreContenant(depart);
    jourActif.value = depart;
    const rect = champ.value?.getBoundingClientRect();
    aDroite.value = rect !== undefined && rect.left + 320 > window.innerWidth;
    auDessus.value =
        rect !== undefined &&
        rect.bottom + HAUTEUR_CALENDRIER > window.innerHeight &&
        rect.top > window.innerHeight - rect.bottom;
    ouvert.value = true;
    document.addEventListener("mousedown", surClicDehors);
    focaliserJourActif();
}

function fermer(rendreLeFocus: boolean) {
    ouvert.value = false;
    document.removeEventListener("mousedown", surClicDehors);
    if (rendreLeFocus) declencheur.value?.focus();
}

function basculer() {
    if (ouvert.value) fermer(false);
    else ouvrir();
}

function surClicDehors(event: MouseEvent) {
    if (!champ.value?.contains(event.target as Node)) fermer(false);
}

onUnmounted(() => document.removeEventListener("mousedown", surClicDehors));

// ── Choix ──

function choisir(iso: string) {
    if (props.min !== "" && iso < props.min) return;
    date.value = iso;
    fermer(true);
}

function effacer() {
    date.value = "";
    fermer(true);
}

function changerFenetre(sens: -1 | 1) {
    if (!fenetre.value) return;
    const voisin =
        sens === 1
            ? ajouterJours(fenetre.value.fin, 1)
            : ajouterJours(fenetre.value.debut, -1);
    fenetre.value = fenetreContenant(voisin);
    // Le focus clavier passe au jour le plus proche dans la nouvelle fenêtre
    jourActif.value = sens === 1 ? fenetre.value.debut : fenetre.value.fin;
}

// ── Clavier : flèches, Pg préc./suiv. pour changer de période, Entrée, Échap ──

async function focaliserJourActif() {
    await nextTick();
    grille.value
        ?.querySelector<HTMLButtonElement>(`[data-iso="${jourActif.value}"]`)
        ?.focus();
}

function deplacer(nb: number) {
    jourActif.value = ajouterJours(jourActif.value, nb);
    if (
        fenetre.value &&
        (jourActif.value < fenetre.value.debut ||
            jourActif.value > fenetre.value.fin)
    ) {
        fenetre.value = fenetreContenant(jourActif.value);
    }
    focaliserJourActif();
}

function surTouche(event: KeyboardEvent) {
    const deplacements: Record<string, number> = {
        ArrowLeft: -1,
        ArrowRight: 1,
        ArrowUp: -7,
        ArrowDown: 7,
    };
    if (event.key in deplacements) {
        event.preventDefault();
        deplacer(deplacements[event.key]);
    } else if (event.key === "PageUp" || event.key === "PageDown") {
        event.preventDefault();
        changerFenetre(event.key === "PageUp" ? -1 : 1);
        focaliserJourActif();
    } else if (event.key === "Escape") {
        // Échap ferme d'abord le calendrier, puis la fenêtre
        event.preventDefault();
        event.stopPropagation();
        fermer(true);
    }
}
</script>

<template>
    <div ref="champ" class="champ champ-date">
        <label :for="id">{{ label }}</label>
        <button
            :id="id"
            ref="declencheur"
            type="button"
            class="declencheur"
            :class="{ 'sans-date': !date }"
            aria-haspopup="dialog"
            :aria-expanded="ouvert"
            @click="basculer"
        >
            <CalendarDays aria-hidden="true" />
            <span>{{ texteChamp }}</span>
        </button>

        <div
            v-if="ouvert && fenetre"
            class="calendrier"
            :class="{ 'a-droite': aDroite, 'au-dessus': auDessus }"
            role="dialog"
            :aria-label="`Choisir : ${label}`"
            @keydown="surTouche"
        >
            <div class="entete">
                <button
                    type="button"
                    class="btn btn-icone"
                    aria-label="Précédent"
                    @click="changerFenetre(-1)"
                >
                    <ChevronLeft aria-hidden="true" />
                </button>
                <div class="titre-calendrier">
                    <strong>{{ fenetre.titre }}</strong>
                    <span>{{ sousTitre }}</span>
                </div>
                <button
                    type="button"
                    class="btn btn-icone"
                    aria-label="Suivant"
                    @click="changerFenetre(1)"
                >
                    <ChevronRight aria-hidden="true" />
                </button>
            </div>

            <div class="jours-semaine" aria-hidden="true">
                <span v-for="(j, i) in JOURS_SEMAINE" :key="i">{{ j }}</span>
            </div>

            <div ref="grille" class="grille">
                <button
                    v-for="jour in jours"
                    :key="jour.iso"
                    type="button"
                    class="jour"
                    :class="{
                        hors: !jour.dansFenetre,
                        aujourdhui: jour.iso === aujourdhui(),
                        choisi: jour.iso === date,
                    }"
                    :data-iso="jour.iso"
                    :tabindex="jour.iso === jourActif ? 0 : -1"
                    :disabled="jour.desactive"
                    :aria-label="jour.libelle"
                    :aria-pressed="jour.iso === date"
                    @click="choisir(jour.iso)"
                >
                    <span v-if="jour.mois" class="mois">{{ jour.mois }}</span>
                    {{ jour.numero }}
                </button>
            </div>

            <div class="pied">
                <button
                    v-if="facultatif && date"
                    type="button"
                    class="btn btn-discret"
                    @click="effacer"
                >
                    Effacer
                </button>
                <button
                    type="button"
                    class="btn btn-discret"
                    :disabled="min !== '' && aujourdhui() < min"
                    @click="choisir(aujourdhui())"
                >
                    Aujourd'hui
                </button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.champ-date {
    position: relative;
}

/* Même allure qu'un champ de saisie */
.declencheur {
    display: flex;
    align-items: center;
    gap: 10px;
    height: 46px;
    min-width: 0;
    padding: 0 14px;
    border: 1px solid var(--trait-champ);
    border-radius: var(--rayon-m);
    background: var(--blanc);
    color: var(--encre);
    font: inherit;
    font-size: 15px;
    font-weight: 500;
    text-align: left;
    cursor: pointer;
}

.declencheur span {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.declencheur svg {
    width: 18px;
    height: 18px;
    flex: none;
    color: var(--vert-700);
}

.declencheur.sans-date {
    color: #7b8b82;
}

.declencheur:focus-visible,
.declencheur[aria-expanded="true"] {
    outline: none;
    border-color: var(--vert-700);
    box-shadow: 0 0 0 3px #1b5c4226;
}

.calendrier {
    position: absolute;
    top: 100%;
    left: 0;
    z-index: 2;
    width: 320px;
    max-width: calc(100vw - 32px);
    margin-top: 6px;
    padding: 14px;
    border-radius: var(--rayon-l);
    background: var(--blanc);
    box-shadow: var(--ombre-fenetre);
}

.calendrier.a-droite {
    left: auto;
    right: 0;
}

/* Au-dessus du bouton (46 px + l'écart), par-dessus le libellé du champ */
.calendrier.au-dessus {
    top: auto;
    bottom: 52px;
    margin-top: 0;
}

.entete {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 10px;
}

.titre-calendrier {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
}

.titre-calendrier strong {
    font-size: 15px;
    font-weight: 800;
}

.titre-calendrier span {
    font-size: 12px;
    font-weight: 500;
    color: var(--encre-douce);
}

.jours-semaine,
.grille {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: 2px;
}

.jours-semaine {
    margin-bottom: 4px;
    font-size: 11px;
    font-weight: 700;
    color: var(--encre-douce);
    text-align: center;
}

.jour {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 40px;
    padding: 0;
    border: none;
    border-radius: var(--rayon-s);
    background: none;
    color: var(--encre);
    font: inherit;
    font-size: 14px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    line-height: 1.1;
    cursor: pointer;
}

/* Petit repère du mois au-dessus du numéro */
.jour .mois {
    font-size: 9px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.02em;
    color: var(--vert-700);
}

.jour:hover:not(:disabled) {
    background: var(--vert-50);
    color: var(--vert-700);
}

.jour:focus-visible {
    outline: none;
    box-shadow: 0 0 0 2px var(--vert-700);
}

/* Jours qui complètent les semaines, hors de la période */
.jour.hors {
    color: #a3b1a9;
    font-weight: 500;
}

.jour.aujourdhui {
    box-shadow: inset 0 0 0 1.5px var(--vert-300);
    font-weight: 800;
}

.jour.choisi,
.jour.choisi:hover {
    background: var(--vert-700);
    color: var(--blanc);
}

.jour.choisi .mois {
    color: var(--vert-200);
}

.jour:disabled {
    opacity: 0.35;
    cursor: not-allowed;
}

.pied {
    display: flex;
    justify-content: flex-end;
    gap: 4px;
    margin-top: 10px;
    padding-top: 8px;
    border-top: 1px solid var(--trait);
}

.pied .btn {
    min-height: 36px;
    padding: 0 12px;
}
</style>
