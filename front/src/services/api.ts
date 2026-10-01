import axios from "axios";
import router from "@/router";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";

const api = axios.create({
    baseURL: "http://localhost:8085/api",
});

api.interceptors.request.use((config) => {
    const auth = useAuthStore();
    if (auth.token) {
        config.headers.Authorization = `Bearer ${auth.token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            const auth = useAuthStore();
            // Un 401 sans session (ex : mauvais mot de passe) n'est pas une expiration
            if (auth.isAuthenticated) {
                auth.logout();
                useNotificationStore().error(
                    "Votre session a expiré. Veuillez vous reconnecter."
                );
                router.replace({ name: "login" });
            }
        }
        return Promise.reject(error);
    }
);

export default api;
