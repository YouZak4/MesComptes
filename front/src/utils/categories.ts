import {
    Baby,
    BookOpen,
    Briefcase,
    Bus,
    Car,
    Coins,
    Dog,
    Dumbbell,
    Fuel,
    Gamepad2,
    Gift,
    GraduationCap,
    Handshake,
    HeartPulse,
    House,
    Landmark,
    type LucideIcon,
    Music,
    Package,
    PaintRoller,
    Percent,
    PiggyBank,
    Plane,
    ShieldCheck,
    Shirt,
    ShoppingCart,
    Smartphone,
    Sparkles,
    Tv,
    Undo2,
    Utensils,
    Wallet,
} from "@lucide/vue";
import type { Categorie, TypeOperation } from "@/types/comptes.ts";

// Icônes proposées pour une catégorie : le back stocke leur nom Lucide
export const ICONES: Record<string, LucideIcon> = {
    tv: Tv,
    "shield-check": ShieldCheck,
    package: Package,
    landmark: Landmark,
    gift: Gift,
    fuel: Fuel,
    "shopping-cart": ShoppingCart,
    house: House,
    "paint-roller": PaintRoller,
    percent: Percent,
    "heart-pulse": HeartPulse,
    sparkles: Sparkles,
    utensils: Utensils,
    dumbbell: Dumbbell,
    smartphone: Smartphone,
    plane: Plane,
    car: Car,
    briefcase: Briefcase,
    handshake: Handshake,
    "undo-2": Undo2,
    "piggy-bank": PiggyBank,
    wallet: Wallet,
    coins: Coins,
    dog: Dog,
    baby: Baby,
    "graduation-cap": GraduationCap,
    "book-open": BookOpen,
    music: Music,
    "gamepad-2": Gamepad2,
    shirt: Shirt,
    bus: Bus,
};

// Nom inconnu (ancien emoji, icône retirée) : l'icône neutre
export function composantIcone(nom: string | null | undefined): LucideIcon {
    return (nom && ICONES[nom]) || Package;
}

export interface Teinte {
    couleur: string;
    teinte: string;
}

// Les 17 couples de la charte : couleur du trait (contraste ≥ 4,5:1 sur sa teinte) et fond de pastille
export const COULEURS_DEPENSES: Teinte[] = [
    { couleur: "#3F51A3", teinte: "#E4E8F6" },
    { couleur: "#4A6378", teinte: "#E3E9EE" },
    { couleur: "#6B7079", teinte: "#ECEDEF" },
    { couleur: "#4F5A66", teinte: "#E7EAED" },
    { couleur: "#A8335F", teinte: "#F6E3EA" },
    { couleur: "#B8501A", teinte: "#F8E4D8" },
    { couleur: "#9A6410", teinte: "#F8EBD3" },
    { couleur: "#8A5A44", teinte: "#F1E6E0" },
    { couleur: "#6B7A1E", teinte: "#EEF0D9" },
    { couleur: "#6B5240", teinte: "#EEE7E1" },
    { couleur: "#B83227", teinte: "#F8E1DF" },
    { couleur: "#B23A72", teinte: "#F7E2EC" },
    { couleur: "#A86400", teinte: "#FBEBD0" },
    { couleur: "#0F7A70", teinte: "#DAF0ED" },
    { couleur: "#1F64B5", teinte: "#E0EBF8" },
    { couleur: "#0A779F", teinte: "#DCEFF6" },
    { couleur: "#34449A", teinte: "#E3E6F5" },
];

// Toutes les catégories de revenus portent le couple de la marque
export const COULEUR_REVENUS: Teinte = {
    couleur: "#1B5C42",
    teinte: "#DCEFE4",
};

const NEUTRE: Teinte = { couleur: "#6B7079", teinte: "#ECEDEF" };

// Couleur absente : neutre. Couleur hors charte (ancienne catégorie personnelle) : teinte calculée
export function teinteDe(
    couleur: string | null | undefined,
    type?: TypeOperation
): Teinte {
    if (type === "REVENU") return COULEUR_REVENUS;
    if (!couleur) return NEUTRE;
    const connue = COULEURS_DEPENSES.find(
        (t) => t.couleur.toLowerCase() === couleur.toLowerCase()
    );
    return (
        connue ?? {
            couleur,
            teinte: `color-mix(in srgb, ${couleur} 14%, white)`,
        }
    );
}

export function teinteCategorie(c: Categorie | null | undefined): Teinte {
    return c ? teinteDe(c.couleur, c.type) : NEUTRE;
}
