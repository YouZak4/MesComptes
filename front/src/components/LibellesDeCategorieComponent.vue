<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { comptesService } from "@/services/comptesService.ts";
import type { Libelle } from "@/types/comptes.ts";

// Libellés visibles de la catégorie choisie, à choisir d'un clic quand on ne retrouve pas le nom à taper
const props = defineProps<{ categorieId: number | null }>();
const emit = defineEmits<{ choisir: [libelle: Libelle] }>();

const libelles = ref<Libelle[]>([]);

// Chargés à chaque ouverture du formulaire : un libellé a pu être créé entre-temps
onMounted(async () => {
    try {
        libelles.value = (await comptesService.getLibelles())
            .filter((l) => !l.masque && !l.archive)
            .sort((a, b) => a.nom.localeCompare(b.nom, "fr"));
    } catch (e) {
        // Sans cette liste, la saisie au clavier reste possible
        console.error(e);
    }
});

const libellesCategorie = computed(() =>
    libelles.value.filter((l) => l.categorieId === props.categorieId)
);
</script>

<template>
    <div v-if="libellesCategorie.length > 0" class="libelles-categorie">
        <p class="aide">Libellés de cette catégorie</p>
        <ul>
            <li v-for="libelle in libellesCategorie" :key="libelle.id">
                <button type="button" @click="emit('choisir', libelle)">
                    {{ libelle.nom }}
                </button>
            </li>
        </ul>
    </div>
</template>

<style scoped>
.libelles-categorie {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

ul {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 0;
    padding: 0;
    list-style: none;
    max-height: 7.5rem;
    overflow-y: auto;
}

button {
    padding: 6px 12px;
    border: 1px solid var(--trait-champ);
    border-radius: 999px;
    background: var(--blanc);
    color: var(--encre);
    font: inherit;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
}

button:hover,
button:focus-visible {
    border-color: var(--vert-700);
    background: var(--vert-50);
    color: var(--vert-700);
}

button:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px #1b5c4226;
}
</style>
