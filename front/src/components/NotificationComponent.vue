<script setup lang="ts">
import { useNotificationStore } from "@/stores/notification";

const notificationStore = useNotificationStore();
</script>

<template>
    <Teleport to="body">
        <div class="notification-container" aria-live="polite">
            <TransitionGroup name="notification">
                <div
                    v-for="notification in notificationStore.notifications"
                    :key="notification.id"
                    class="notification"
                    :class="notification.type"
                    :role="notification.type === 'error' ? 'alert' : 'status'"
                >
                    <span class="notification-icon">
                        {{ notification.type === "success" ? "✔" : "✖" }}
                    </span>
                    <p class="notification-message">
                        {{ notification.message }}
                    </p>
                    <button
                        type="button"
                        class="notification-close"
                        aria-label="Fermer"
                        @click="notificationStore.remove(notification.id)"
                    >
                        ×
                    </button>
                </div>
            </TransitionGroup>
        </div>
    </Teleport>
</template>

<style scoped>
.notification-container {
    position: fixed;
    top: 1.5rem;
    left: 50%;
    transform: translateX(-50%);
    z-index: 1000;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    width: min(90vw, 420px);
    pointer-events: none;
}

.notification {
    font-family: Arial, Helvetica, sans-serif;
    font-size: 1rem;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    border: 1px solid;
    border-left-width: 6px;
    border-radius: 10px;
    background: #e3d3f1;
    color: #45335c;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    pointer-events: auto;
}

.notification.success {
    border-color: #14a81a;
}

.notification.error {
    border-color: #c62727;
}

.notification-icon {
    font-weight: bold;
}

.notification.success .notification-icon {
    color: #14a81a;
}

.notification.error .notification-icon {
    color: #c62727;
}

.notification-message {
    flex: 1;
    margin: 0;
    text-align: left;
}

.notification-close {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 1.25rem;
    line-height: 1;
    color: #45335c;
}

/* Animations d'apparition / disparition */
.notification-enter-active,
.notification-leave-active {
    transition:
        opacity 0.3s,
        transform 0.3s;
}

.notification-enter-from,
.notification-leave-to {
    opacity: 0;
    transform: translateY(-1rem);
}
</style>
