<script setup lang="ts">
import { CircleAlert, CircleCheck, X } from "@lucide/vue";
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
                    <span class="notification-icon" aria-hidden="true">
                        <CircleCheck
                            v-if="notification.type === 'success'"
                            :size="20"
                        />
                        <CircleAlert v-else :size="20" />
                    </span>
                    <p class="notification-message">
                        {{ notification.message }}
                    </p>
                    <button
                        type="button"
                        class="btn btn-icone"
                        aria-label="Fermer"
                        @click="notificationStore.remove(notification.id)"
                    >
                        <X aria-hidden="true" />
                    </button>
                </div>
            </TransitionGroup>
        </div>
    </Teleport>
</template>

<style scoped>
.notification-container {
    position: fixed;
    top: 76px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 1000;
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: min(calc(100vw - 32px), 440px);
    pointer-events: none;
}

.notification {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 8px 8px 16px;
    border-radius: var(--rayon-l);
    background: var(--blanc);
    color: var(--encre);
    font-weight: 600;
    box-shadow: var(--ombre-flottante);
    pointer-events: auto;
}

.notification-icon {
    display: flex;
    flex: none;
}

.notification.success .notification-icon {
    color: var(--positif);
}

.notification.error .notification-icon {
    color: var(--negatif);
}

.notification-message {
    flex: 1;
    margin: 0;
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
