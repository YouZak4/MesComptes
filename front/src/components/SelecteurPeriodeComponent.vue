<script setup lang="ts">
import { ChevronLeft, ChevronRight } from "@lucide/vue";

// Capsule blanche : période précédente, retour à la période en cours, période suivante
defineProps<{
    precedente: boolean;
    suivante: boolean;
    enCours: boolean;
}>();
const emit = defineEmits<{
    precedente: [];
    suivante: [];
    courante: [];
}>();
</script>

<template>
    <div class="selecteur-periode" role="group" aria-label="Période">
        <button
            type="button"
            class="btn btn-icone"
            aria-label="Période précédente"
            :disabled="!precedente"
            @click="emit('precedente')"
        >
            <ChevronLeft aria-hidden="true" />
        </button>
        <button
            type="button"
            class="btn courante"
            :disabled="enCours"
            :aria-pressed="enCours"
            @click="emit('courante')"
        >
            Période en cours
        </button>
        <button
            type="button"
            class="btn btn-icone"
            aria-label="Période suivante"
            :disabled="!suivante"
            @click="emit('suivante')"
        >
            <ChevronRight aria-hidden="true" />
        </button>
    </div>
</template>

<style scoped>
.selecteur-periode {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 4px;
    border-radius: var(--rayon-l);
    background: var(--blanc);
    box-shadow: var(--ombre-carte);
}

.courante {
    min-height: 40px;
    padding: 0 12px;
    border-radius: var(--rayon-s);
    color: var(--vert-700);
}

.courante:hover:not(:disabled) {
    background: var(--vert-50);
}

/* Déjà sur la période en cours : simple libellé, pas un bouton grisé */
.courante:disabled {
    opacity: 1;
    cursor: default;
    color: var(--encre);
}
</style>
