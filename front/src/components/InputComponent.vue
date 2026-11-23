<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, useId, watch } from "vue";
import { CircleAlert, Eye, EyeOff } from "@lucide/vue";

const props = withDefaults(
    defineProps<{
        modelValue: string;
        type?: string;
        label?: string;
        placeholder?: string;
        required?: boolean;
        disabled?: boolean;
        regex?: RegExp;
        regexMessage?: string;
        // Contrôle libre, pour ce qu'une regex ne sait pas dire : renvoie le message d'erreur ou null
        verifier?: (valeur: string) => string | null;
        minLength?: number;
        maxLength?: number;
        // Valeur à recopier à l'identique (confirmation d'un mot de passe)
        correspondA?: string;
        correspondAMessage?: string;
    }>(),
    {
        type: "text",
        label: "",
        placeholder: "",
        required: false,
        disabled: false,
        regexMessage: "Format invalide.",
        correspondAMessage: "Les deux saisies ne correspondent pas.",
    }
);
const emit = defineEmits<{
    (e: "update:modelValue", value: string): void;
    (e: "valid", value: boolean): void;
}>();

// Relie le libellé et le message d'erreur au champ
const id = useId();
const touched = ref(false);
const showPassword = ref(false);

const inputType = computed(() => {
    if (props.type === "password") {
        return showPassword.value ? "text" : "password";
    }
    return props.type;
});

// Erreur de la valeur courante, que le champ ait déjà perdu le focus ou non
const erreur = computed((): string | null => {
    const val = props.modelValue;

    if (props.required && val.trim() === "") {
        return "Ce champ est obligatoire.";
    }
    if (props.minLength && val.length < props.minLength) {
        return `Minimum ${props.minLength} caractères.`;
    }
    if (props.maxLength && val.length > props.maxLength) {
        return `Maximum ${props.maxLength} caractères.`;
    }
    if (props.regex && !props.regex.test(val)) {
        return props.regexMessage ?? "Format invalide.";
    }
    const erreurVerifiee = props.verifier?.(val) ?? null;
    if (erreurVerifiee !== null) {
        return erreurVerifiee;
    }
    if (props.correspondA !== undefined && val !== props.correspondA) {
        return props.correspondAMessage;
    }

    return null;
});

// Le message ne s'affiche qu'une fois le champ quitté, pour ne pas gêner la saisie
const errorMessage = computed(() => (touched.value ? erreur.value : null));

const isValid = computed(() => {
    if (!touched.value) return null;
    return errorMessage.value === null;
});

// La validité, elle, suit chaque changement de valeur (saisie, remise à zéro par le parent…)
// pour qu'un formulaire ne puisse pas être envoyé avec une saisie au mauvais format
const estValide = computed(
    () => erreur.value === null && props.modelValue.trim() !== ""
);
watch(estValide, (valide) => emit("valid", valide), { immediate: true });

function onInput(event: Event) {
    const target = event.target as HTMLInputElement;
    emit("update:modelValue", target.value);
}

// Bouton de la souris (ou doigt) enfoncé quelque part dans la page
let pointeurEnfonce = false;
function surPointeurEnfonce() {
    pointeurEnfonce = true;
}
function surPointeurRelache() {
    pointeurEnfonce = false;
}
onMounted(() => {
    document.addEventListener("pointerdown", surPointeurEnfonce, true);
    document.addEventListener("pointerup", surPointeurRelache, true);
    document.addEventListener("pointercancel", surPointeurRelache, true);
});
onUnmounted(() => {
    document.removeEventListener("pointerdown", surPointeurEnfonce, true);
    document.removeEventListener("pointerup", surPointeurRelache, true);
    document.removeEventListener("pointercancel", surPointeurRelache, true);
});

function onBlur() {
    if (!pointeurEnfonce) {
        touched.value = true;
        return;
    }
    // Champ quitté par un clic ailleurs : le message n'apparaît qu'une fois le clic terminé.
    // Affiché tout de suite, il décalerait la page sous le pointeur et le clic tomberait à côté
    const fin = new AbortController();
    const afficher = () => {
        fin.abort();
        setTimeout(() => (touched.value = true));
    };
    document.addEventListener("pointerup", afficher, { signal: fin.signal });
    document.addEventListener("pointercancel", afficher, {
        signal: fin.signal,
    });
}
</script>

<template>
    <div class="champ" :class="{ 'champ-invalide': isValid === false }">
        <label v-if="label" :for="id">
            {{ label }}
            <span v-if="required" class="obligatoire" aria-hidden="true"
                >*</span
            >
        </label>

        <div class="zone">
            <input
                :id="id"
                :type="inputType"
                :value="modelValue"
                :placeholder="placeholder"
                :required="required"
                :disabled="disabled"
                :maxlength="maxLength"
                :aria-invalid="isValid === false"
                :aria-describedby="errorMessage ? `${id}-erreur` : undefined"
                @input="onInput"
                @blur="onBlur"
            />

            <!-- Bouton d'affichage uniquement pour les champs mot de passe -->
            <button
                v-if="type === 'password'"
                type="button"
                class="btn btn-icone oeil"
                :aria-label="
                    showPassword
                        ? 'Masquer le mot de passe'
                        : 'Afficher le mot de passe'
                "
                :aria-pressed="showPassword"
                @click="showPassword = !showPassword"
            >
                <EyeOff v-if="showPassword" aria-hidden="true" />
                <Eye v-else aria-hidden="true" />
            </button>
        </div>

        <!-- Message d'erreur, affiché seulement si touched + erreur -->
        <p v-if="errorMessage" :id="`${id}-erreur`" class="erreur">
            <CircleAlert aria-hidden="true" />
            {{ errorMessage }}
        </p>
    </div>
</template>

<style scoped>
.obligatoire {
    color: var(--negatif);
}

.zone {
    position: relative;
    display: flex;
}

.zone input {
    flex: 1;
    width: 100%;
}

.zone input[type="password"],
.zone:has(.oeil) input {
    padding-right: 48px;
}

.oeil {
    position: absolute;
    right: 3px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--encre-douce);
}
</style>
