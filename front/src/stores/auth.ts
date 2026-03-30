import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useAuthStore = defineStore("auth", () => {
    function parseRoles(): string[] {
        try {
            return JSON.parse(localStorage.getItem("roles") ?? "[]");
        } catch {
            return [];
        }
    }
    const token = ref<string>(localStorage.getItem("token") ?? "");
    const identifiant = ref<string>(localStorage.getItem("identifiant") ?? "");
    const roles = ref<string[]>(parseRoles());

    const isAuthenticated = computed(() => !!token.value);

    // Action : connexion
    function setAuth(data: {
        token: string;
        identifiant: string;
        roles: string[];
    }) {
        token.value = data.token;
        identifiant.value = data.identifiant;
        roles.value = data.roles;

        // On persiste dans localStorage
        localStorage.setItem("token", data.token);
        localStorage.setItem("identifiant", data.identifiant);
        localStorage.setItem("roles", JSON.stringify(data.roles));
    }

    // Action : déconnexion
    function logout() {
        token.value = "";
        identifiant.value = "";
        roles.value = [];

        localStorage.removeItem("token");
        localStorage.removeItem("identifiant");
        localStorage.removeItem("roles");
    }

    return { token, identifiant, roles, isAuthenticated, setAuth, logout };
});
