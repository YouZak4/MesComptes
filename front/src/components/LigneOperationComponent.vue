<script setup lang="ts">
import PastilleCategorieComponent from "@/components/PastilleCategorieComponent.vue";
import type { Categorie, TypeOperation } from "@/types/comptes.ts";
import { formatMontantSigne } from "@/utils/format.ts";

// [pastille] [libellé / méta] …… [montant] [actions] : opération, occurrence à venir ou récurrence
defineProps<{
    categorie: Categorie | undefined;
    titre: string;
    meta?: string;
    montant: number;
    type: TypeOperation;
    estompee?: boolean;
}>();
</script>

<template>
    <li class="ligne-operation" :class="{ estompee }">
        <PastilleCategorieComponent :categorie="categorie" />
        <div class="texte">
            <span class="titre">
                {{ titre }}
                <!-- Badges (récurrente…) -->
                <slot name="badges" />
            </span>
            <span v-if="meta" class="meta">{{ meta }}</span>
            <slot name="meta" />
        </div>
        <!-- Dépense en encre, précédée du signe moins ; le rouge brique est réservé aux soldes -->
        <span class="montant num" :class="{ positif: type === 'REVENU' }">
            {{ formatMontantSigne(montant, type) }}
        </span>
        <slot name="actions" />
    </li>
</template>

<style scoped>
.ligne-operation {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 12px;
    margin: 0 -8px;
    padding: 11px 8px;
    border-radius: var(--rayon-m);
}

.ligne-operation:hover {
    background: var(--vert-50);
}

.texte {
    flex: 1 1 0;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.titre {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 2px 8px;
    font-weight: 700;
}

.meta {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.montant {
    margin-left: auto;
    font-weight: 800;
    white-space: nowrap;
}

.estompee > :not(:last-child) {
    opacity: 0.6;
}
</style>
