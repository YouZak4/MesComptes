import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "@/stores/auth.ts";

// JWT factice : seule la charge utile (2e partie) est lue par le store, la signature n'est pas vérifiée
function jeton(charge: object): string {
    return `entete.${btoa(JSON.stringify(charge))}.signature`;
}

// exp en secondes, comme dans un vrai JWT
function dansSecondes(secondes: number): number {
    return Math.floor(Date.now() / 1000) + secondes;
}

describe("store auth", () => {
    beforeEach(() => {
        localStorage.clear();
        setActivePinia(createPinia());
    });

    it("n'est pas authentifié sans jeton", () => {
        const auth = useAuthStore();
        expect(auth.isAuthenticated).toBe(false);
        expect(auth.roles).toEqual([]);
    });

    it("lit l'identifiant et les rôles du jeton et les garde en localStorage", () => {
        const auth = useAuthStore();
        const brut = jeton({ sub: "jean@exemple.fr", roles: ["USER"] });

        auth.setAuth(brut);

        expect(auth.isAuthenticated).toBe(true);
        expect(auth.identifiant).toBe("jean@exemple.fr");
        expect(auth.roles).toEqual(["USER"]);
        expect(localStorage.getItem("token")).toBe(brut);
        expect(localStorage.getItem("identifiant")).toBe("jean@exemple.fr");
        expect(localStorage.getItem("roles")).toBe('["USER"]');
    });

    it("retrouve la session enregistrée au rechargement de la page", () => {
        useAuthStore().setAuth(jeton({ sub: "jean", roles: ["USER"] }));

        setActivePinia(createPinia());
        const auth = useAuthStore();

        expect(auth.isAuthenticated).toBe(true);
        expect(auth.identifiant).toBe("jean");
        expect(auth.roles).toEqual(["USER"]);
    });

    it("ignore des rôles illisibles en localStorage", () => {
        localStorage.setItem("roles", "pas du json");
        expect(useAuthStore().roles).toEqual([]);
    });

    it("accepte un jeton illisible sans planter, sans identifiant ni rôle", () => {
        const auth = useAuthStore();
        auth.setAuth("n'importe quoi");
        expect(auth.identifiant).toBe("");
        expect(auth.roles).toEqual([]);
        expect(auth.isTokenExpired()).toBe(true);
    });

    it("vide la session et le localStorage à la déconnexion", () => {
        const auth = useAuthStore();
        auth.setAuth(jeton({ sub: "jean", roles: ["USER"] }));

        auth.logout();

        expect(auth.isAuthenticated).toBe(false);
        expect(auth.identifiant).toBe("");
        expect(auth.roles).toEqual([]);
        expect(localStorage.getItem("token")).toBeNull();
        expect(localStorage.getItem("identifiant")).toBeNull();
        expect(localStorage.getItem("roles")).toBeNull();
    });

    describe("isTokenExpired", () => {
        it("est faux pour un jeton encore valide", () => {
            const auth = useAuthStore();
            auth.setAuth(jeton({ sub: "jean", exp: dansSecondes(3600) }));
            expect(auth.isTokenExpired()).toBe(false);
        });

        it("est vrai pour un jeton expiré", () => {
            const auth = useAuthStore();
            auth.setAuth(jeton({ sub: "jean", exp: dansSecondes(-1) }));
            expect(auth.isTokenExpired()).toBe(true);
        });

        it("est vrai pour un jeton sans date d'expiration", () => {
            const auth = useAuthStore();
            auth.setAuth(jeton({ sub: "jean" }));
            expect(auth.isTokenExpired()).toBe(true);
        });

        it("est vrai sans jeton", () => {
            expect(useAuthStore().isTokenExpired()).toBe(true);
        });
    });
});
