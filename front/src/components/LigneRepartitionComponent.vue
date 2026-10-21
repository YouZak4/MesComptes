<script setup lang="ts">
import PastilleCategorieComponent from "@/components/PastilleCategorieComponent.vue";
import type { Categorie, TotalCategorie } from "@/types/comptes.ts";
import {
    formatEcart,
    formatMontant,
    formatPourcentage,
} from "@/utils/format.ts";

// Une ligne de la légende des dépenses : pastille, nom, montant, part des dépenses
// et, si une période précédente existe, l'écart avec elle
defineProps<{
    nom: string;
    total: number;
    part: number;
    pastille: Pick<Categorie, "icone" | "couleur" | "type">;
    // Absent pour « Autres »
    detail?: TotalCategorie;
    avecEcart: boolean;
}>();

// Plus de dépenses que la période précédente : défavorable
function classeEcart(t: TotalCategorie): string {
    if (t.ecart === 0) return "";
    return t.ecart > 0 ? "negatif" : "positif";
}
</script>

<template>
    <PastilleCategorieComponent :categorie="pastille" />
    <div class="texte">
        <div class="nom-montant">
            <span>{{ nom }}</span>
            <span class="num">{{ formatMontant(total) }}</span>
        </div>
        <span class="meta">
            {{ formatPourcentage(part) }} des dépenses
            <slot name="meta" />
            <template v-if="detail && avecEcart">
                ·
                <span
                    class="ecart num"
                    :class="classeEcart(detail)"
                    :title="`Période précédente : ${formatMontant(detail.totalPrecedent)}`"
                >
                    {{ formatEcart(detail.ecart) }} vs période précédente
                </span>
            </template>
        </span>
    </div>
</template>

<style scoped>
.texte {
    flex: 1;
    min-width: 0;
}

.nom-montant {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-weight: 700;
}

.nom-montant .num {
    white-space: nowrap;
}

.ecart {
    font-weight: 600;
}
</style>
