import { resolve } from "path";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), "");
    return {
        server: {
            port: Number(env.VITE_FRONT_PORT) || 5173,
        },
        resolve: {
            alias: {
                "@": resolve(__dirname, "./src"),
            },
        },
        plugins: [vue()],
        define: {
            global: "window",
        },
    };
});
