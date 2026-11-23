/// <reference types="vitest/config" />
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
        // Tests Vitest : à côté du fichier testé (xxx.test.ts), DOM simulé par jsdom
        test: {
            environment: "jsdom",
            include: ["src/**/*.test.ts"],
        },
    };
});
