import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useProfilStore } from "@/stores/profil.ts";
import { utilisateurService } from "@/services/utilisateurService.ts";
import type { Utilisateur } from "@/types/utilisateur.ts";

// Pas d'appel HTTP dans les tests : le service est remplacé
vi.mock("@/services/utilisateurService.ts", () => ({
    utilisateurService: { getMoi: vi.fn() },
}));

const jean: Utilisateur = {
    id: "u1",
    nom: "Dupont",
    prenom: "Jean",
    email: "jean@exemple.fr",
};

describe("store profil", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setActivePinia(createPinia());
    });

    it("est vide au départ", () => {
        expect(useProfilStore().utilisateur).toBeNull();
    });

    it("charge l'utilisateur connecté depuis l'API", async () => {
        vi.mocked(utilisateurService.getMoi).mockResolvedValue(jean);
        const store = useProfilStore();

        const charge = await store.charger();

        expect(utilisateurService.getMoi).toHaveBeenCalledOnce();
        expect(charge).toEqual(jean);
        expect(store.utilisateur).toEqual(jean);
    });

    it("propage l'erreur de l'API sans changer l'utilisateur", async () => {
        vi.mocked(utilisateurService.getMoi).mockRejectedValue(
            new Error("réseau")
        );
        const store = useProfilStore();

        await expect(store.charger()).rejects.toThrow("réseau");
        expect(store.utilisateur).toBeNull();
    });

    it("met à jour puis vide l'utilisateur", () => {
        const store = useProfilStore();

        store.mettreAJour(jean);
        expect(store.utilisateur).toEqual(jean);

        store.vider();
        expect(store.utilisateur).toBeNull();
    });
});
