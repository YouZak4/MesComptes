import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8085/api",
});

// Intercepteur : ajoute le token JWT à chaque requête automatiquement
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Intercepteur : si le back répond 401, on redirige vers le login
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.clear();
            window.location.href = "/login";
        }
        return Promise.reject(error);
    }
);

export default api;
