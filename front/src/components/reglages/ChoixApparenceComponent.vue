<script setup lang="ts">
import { watch } from "vue";
import type { TypeOperation } from "@/types/comptes.ts";
import {
    COULEUR_REVENUS,
    COULEURS_DEPENSES,
    ICONES,
} from "@/utils/categories.ts";

// Icône et couleur d'une catégorie : seulement les icônes et les couples de la charte, sans saisie libre
const props = defineProps<{ type: TypeOperation; nomCategorie: string }>();
const icone = defineModel<string>("icone", { required: true });
const couleur = defineModel<string>("couleur", { required: true });

const icones = Object.entries(ICONES);

// Une catégorie de revenus porte toujours le couple de la marque
watch(
    () => props.type,
    (type) => {
        if (type === "REVENU") couleur.value = COULEUR_REVENUS.couleur;
        else if (couleur.value === COULEUR_REVENUS.couleur)
            couleur.value = COULEURS_DEPENSES[0].couleur;
    },
    { immediate: true }
);

function teinteActuelle(): string {
    if (props.type === "REVENU") return COULEUR_REVENUS.teinte;
    return (
        COULEURS_DEPENSES.find((t) => t.couleur === couleur.value)?.teinte ??
        COULEURS_DEPENSES[0].teinte
    );
}
</script>

<template>
    <div class="apparence">
        <fieldset>
            <legend>Icône</legend>
            <div class="grille" role="radiogroup">
                <label
                    v-for="[nom, composant] in icones"
                    :key="nom"
                    class="option-icone"
                    :class="{ choisie: icone === nom }"
                    :style="
                        icone === nom
                            ? { background: teinteActuelle(), color: couleur }
                            : undefined
                    "
                >
                    <input
                        v-model="icone"
                        type="radio"
                        :name="`icone-${nomCategorie}`"
                        :value="nom"
                        :aria-label="nom"
                    />
                    <component :is="composant" :size="19" aria-hidden="true" />
                </label>
            </div>
        </fieldset>

        <fieldset v-if="type === 'DEPENSE'">
            <legend>Couleur</legend>
            <div class="grille" role="radiogroup">
                <label
                    v-for="(t, index) in COULEURS_DEPENSES"
                    :key="t.couleur"
                    class="option-couleur"
                    :class="{ choisie: couleur === t.couleur }"
                    :style="{ background: t.teinte, color: t.couleur }"
                >
                    <input
                        v-model="couleur"
                        type="radio"
                        :name="`couleur-${nomCategorie}`"
                        :value="t.couleur"
                        :aria-label="`Couleur ${index + 1}`"
                    />
                    <span class="point" aria-hidden="true"></span>
                </label>
            </div>
        </fieldset>
        <p v-else class="aide">
            Les catégories de revenus portent toutes le vert de la marque.
        </p>
    </div>
</template>

<style scoped>
.apparence {
    flex-basis: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

fieldset {
    margin: 0;
    padding: 0;
    border: 0;
    min-width: 0;
}

legend {
    margin-bottom: 6px;
    padding: 0;
    font-size: 13px;
    font-weight: 700;
}

.grille {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.option-icone,
.option-couleur {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    border-radius: var(--rayon-m);
    cursor: pointer;
}

.option-icone {
    color: var(--encre-douce);
}

.option-icone:hover {
    background: var(--vert-50);
}

/* Le bouton radio reste accessible au clavier, mais invisible */
input[type="radio"] {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    margin: 0;
    opacity: 0;
    cursor: pointer;
}

.option-icone:has(:focus-visible),
.option-couleur:has(:focus-visible) {
    outline: 2px solid var(--vert-700);
    outline-offset: 2px;
}

.choisie {
    box-shadow: 0 0 0 2px var(--vert-700);
}

.point {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: currentColor;
}
</style>
