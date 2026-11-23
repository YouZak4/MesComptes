import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useNotificationStore } from "@/stores/notification.ts";

describe("store notification", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        setActivePinia(createPinia());
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it("affiche une notification de succès et une d'erreur", () => {
        const store = useNotificationStore();

        store.success("Opération enregistrée");
        store.error("Échec");

        expect(store.notifications).toEqual([
            { id: 0, type: "success", message: "Opération enregistrée" },
            { id: 1, type: "error", message: "Échec" },
        ]);
    });

    it("retire la notification après 4 secondes par défaut", () => {
        const store = useNotificationStore();
        store.success("Enregistré");

        vi.advanceTimersByTime(3999);
        expect(store.notifications).toHaveLength(1);

        vi.advanceTimersByTime(1);
        expect(store.notifications).toHaveLength(0);
    });

    it("respecte une durée personnalisée", () => {
        const store = useNotificationStore();
        store.error("Erreur", 1000);

        vi.advanceTimersByTime(1000);
        expect(store.notifications).toHaveLength(0);
    });

    it("garde affichée une notification de durée 0", () => {
        const store = useNotificationStore();
        store.error("À fermer à la main", 0);

        vi.advanceTimersByTime(60_000);
        expect(store.notifications).toHaveLength(1);
    });

    it("ne retire que la notification fermée", () => {
        const store = useNotificationStore();
        store.success("Première", 0);
        store.success("Seconde", 0);

        store.remove(0);

        expect(store.notifications.map((n) => n.message)).toEqual(["Seconde"]);
    });
});
