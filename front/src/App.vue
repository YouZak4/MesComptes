<script setup lang="ts">
import NotificationComponent from "@/components/NotificationComponent.vue";
</script>

<template>
    <RouterView />
    <NotificationComponent />
</template>

<style>
body {
    margin: 0;
    font-family: Arial, Helvetica, sans-serif;
}
</style>
