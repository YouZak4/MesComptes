import { createRouter, createWebHistory } from "vue-router";
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
        { path: "/login", name: "login", component: LoginView },
    ],
});

// Guard global : redirige vers /login si pas de token
router.beforeEach((to) => {
    const token = localStorage.getItem("token");
    if (to.meta.requiresAuth && !token) {
        return { name: "login" };
    }
});

export default router;
