import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import PageAccueil from "@/views/pageAccueil.vue";
import LoginView from "@/views/loginView.vue";

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
    ],
});

router.beforeEach((to) => {
    const auth = useAuthStore();

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return { name: "login" };
    }

    if (auth.isAuthenticated && to.name === "login") {
        return { name: "accueil" };
    }
});

export default router;
