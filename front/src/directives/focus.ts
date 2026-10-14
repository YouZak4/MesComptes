import type { Directive } from "vue";

// v-focus : place le curseur dans le champ dès qu'il apparaît (formulaire ouvert dans une ligne)
export const vFocus: Directive<HTMLElement> = {
    mounted: (el) => el.focus(),
};
