import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import PageAccueil from "@/views/pageAccueil.vue";
import LoginView from "@/views/loginView.vue";
import VerificationView from "@/views/verificationView.vue";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: "/",
            name: "accueil",
            component: PageAccueil,
            meta: { requiresAuth: true },
        },
        {
            path: "/login",
            name: "login",
            component: LoginView,
        },
        {
            path: "/verification/:jeton",
            name: "verification",
            component: VerificationView,
        },
    ],
});

router.beforeEach((to) => {
    const auth = useAuthStore();

    // Un token expiré ne doit plus compter comme une session active
    if (auth.isAuthenticated && auth.isTokenExpired()) {
        auth.logout();
    }

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return { name: "login" };
    }

    if (
        auth.isAuthenticated &&
        (to.name === "login" || to.name === "verification")
    ) {
        return { name: "accueil" };
    }
});

export default router;
