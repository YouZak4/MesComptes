<script setup lang="ts">
import { computed } from "vue";
import type { Categorie } from "@/types/comptes.ts";
import { composantIcone, teinteDe } from "@/utils/categories.ts";

// Carré de 38 px : fond à la teinte de la catégorie, icône à sa couleur. Décoratif : le nom est toujours écrit à côté
const props = defineProps<{
    categorie: Pick<Categorie, "icone" | "couleur" | "type"> | null | undefined;
}>();

const icone = computed(() => composantIcone(props.categorie?.icone));
const teinte = computed(() =>
    teinteDe(props.categorie?.couleur, props.categorie?.type)
);
</script>

<template>
    <span
        class="pastille-categorie"
        :style="{ background: teinte.teinte, color: teinte.couleur }"
        aria-hidden="true"
    >
        <component :is="icone" :size="19" :stroke-width="1.9" />
    </span>
</template>

<style scoped>
.pastille-categorie {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 38px;
    height: 38px;
    border-radius: var(--rayon-m);
}
</style>
