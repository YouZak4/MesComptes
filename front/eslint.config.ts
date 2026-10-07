import tsEslint from "@typescript-eslint/parser";
import vueParser from "vue-eslint-parser";
import pluginVue from "eslint-plugin-vue";
import pluginPrettier from "eslint-plugin-prettier";

export default [
    {
        ignores: ["**/node_modules/**", "**/dist/**", "**/build/**"],
    },
    {
        files: ["**/*.{ts,vue}"],
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
            parser: tsEslint,
        },
        rules: {
            "prettier/prettier": ["error", { semi: true }],
        },
        plugins: {
            prettier: pluginPrettier,
        },
    },
    ...pluginVue.configs["flat/essential"],
    {
        files: ["**/*.vue"],
        languageOptions: {
            parser: vueParser,
            parserOptions: {
                parser: tsEslint,
            },
        },
        rules: {
            "vue/multi-word-component-names": "off",
        },
    },
];
