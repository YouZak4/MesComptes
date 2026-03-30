import { defineStore } from "pinia";
import { ref, computed } from "vue";

function decodeToken(token: string) {
    try {
        console.log("token", token);
        const payload = token.split(".")[1];
        return JSON.parse(atob(payload));
    } catch {
        return null;
    }
}

export const useAuthStore = defineStore("auth", () => {
    const token = ref<string>(localStorage.getItem("token") ?? "");
    const identifiant = ref<string>(localStorage.getItem("identifiant") ?? "");
    function parseRoles(): string[] {
        try {
            return JSON.parse(localStorage.getItem("roles") ?? "[]");
        } catch {
            return [];
        }
    }
    const roles = ref<string[]>(parseRoles());

    const isAuthenticated = computed(() => !!token.value);

    function setAuth(rawToken: string) {
        const payload = decodeToken(rawToken);

        token.value = rawToken;
        identifiant.value = payload?.sub ?? "";
        roles.value = payload?.roles ?? [];

        localStorage.setItem("token", rawToken);
        localStorage.setItem("identifiant", identifiant.value);
        localStorage.setItem("roles", JSON.stringify(roles.value));
    }

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
