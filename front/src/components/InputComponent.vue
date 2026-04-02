<script setup lang="ts">
import { ref, computed } from "vue";

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
        minLength?: number;
        maxLength?: number;
    }>(),
    {
        type: "text",
        label: "",
        placeholder: "",
        required: false,
        disabled: false,
        regexMessage: "Format invalide.",
    }
);
const emit = defineEmits<{
    (e: "update:modelValue", value: string): void;
    (e: "valid", value: boolean): void; // 👈
}>();

const touched = ref(false);
const showPassword = ref(false);

const inputType = computed(() => {
    if (props.type === "password") {
        return showPassword.value ? "text" : "password";
    }
    return props.type;
});

const errorMessage = computed((): string | null => {
    if (!touched.value) return null;

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

    return null;
});

const isValid = computed(() => {
    if (!touched.value) return null;
    return errorMessage.value === null;
});

function onInput(event: Event) {
    const target = event.target as HTMLInputElement;
    emit("update:modelValue", target.value);
    emit("valid", errorMessage.value === null && target.value.trim() !== "");
}

function onBlur() {
    touched.value = true;
    emit(
        "valid",
        errorMessage.value === null && props.modelValue.trim() !== ""
    );
}
</script>

<template>
    <div class="main-input-container">
        <label v-if="label" class="base-input-label">
            {{ label }}
            <span v-if="required" class="required-star">*</span>
        </label>

        <div
            class="base-input-field"
            :class="{ valid: isValid === true, invalid: isValid === false }"
        >
            <input
                :type="inputType"
                :model-value="modelValue"
                :placeholder="placeholder"
                :required="required"
                :disabled="disabled"
                :maxlength="maxLength"
                class="base-input"
                @input="onInput"
                @blur="onBlur"
            />

            <!-- Bouton toggle uniquement pour les champs password -->
            <button
                v-if="type === 'password'"
                type="button"
                class="toggle-password"
                @click="showPassword = !showPassword"
            >
                {{ showPassword ? "👁️" : "🙈" }}
            </button>
        </div>

        <!-- Message d'erreur, affiché seulement si touched + erreur -->
        <p v-if="errorMessage" class="base-input-error">{{ errorMessage }}</p>
    </div>
</template>

<style scoped>
.main-input-container {
    font-family: Arial, Helvetica, sans-serif;
    font-size: 1rem;
    display: flex;
    flex-direction: column;
    width: 100%;
}

.base-input-label {
    color: #45335c;
    text-align: left;
}

.required-star {
    color: #c62727;
}

/* Conteneur input + bouton toggle (pour le password) */
.base-input-field {
    display: flex;
    align-items: center;
    border: 1px solid #45335c;
    border-radius: 10px;
    background: #e3d3f1;
    transition: border-color 0.2s;
}

.base-input-field:focus-within {
    border-color: #888;
}

.base-input-field.valid {
    border-color: #14a81a;
}

.base-input-field.invalid {
    border-color: #c62727;
}

.base-input {
    flex: 1;
    padding: 0.55rem 0.75rem;
    background: transparent;
    border: none;
    outline: none;
    color: #000000;
    font-size: 0.95rem;
}

.base-input::placeholder {
    color: #81758e;
}

.toggle-password {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0 0.6rem;
    font-size: 1rem;
    line-height: 1;
}

.base-input-error {
    margin-top: 0.5rem;
    font-size: 1rem;
    color: #c62727;
    text-align: left;
}
</style>
