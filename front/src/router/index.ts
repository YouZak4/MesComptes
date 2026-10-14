import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import TableauDeBordView from "@/views/tableauDeBordView.vue";
import LoginView from "@/views/loginView.vue";
import VerificationView from "@/views/verificationView.vue";
import OperationsView from "@/views/operationsView.vue";
import RecurrencesView from "@/views/recurrencesView.vue";
import ReglagesView from "@/views/reglagesView.vue";
import BienvenueView from "@/views/bienvenueView.vue";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: "/",
            name: "accueil",
            component: TableauDeBordView,
            meta: { requiresAuth: true },
        },
        {
            path: "/operations",
            name: "operations",
            component: OperationsView,
            meta: { requiresAuth: true },
        },
        {
            path: "/recurrences",
            name: "recurrences",
            component: RecurrencesView,
            meta: { requiresAuth: true },
        },
        {
            path: "/reglages",
            name: "reglages",
            component: ReglagesView,
            meta: { requiresAuth: true },
        },
        {
            path: "/bienvenue",
            name: "bienvenue",
            component: BienvenueView,
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
