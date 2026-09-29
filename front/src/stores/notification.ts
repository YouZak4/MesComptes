import { defineStore } from "pinia";
import { ref } from "vue";

export type NotificationType = "success" | "error";

export interface Notification {
    id: number;
    type: NotificationType;
    message: string;
}

const DUREE_PAR_DEFAUT = 4000;

export const useNotificationStore = defineStore("notification", () => {
    const notifications = ref<Notification[]>([]);
    let prochainId = 0;

    function show(
        type: NotificationType,
        message: string,
        duree: number = DUREE_PAR_DEFAUT
    ) {
        const id = prochainId++;
        notifications.value.push({ id, type, message });

        // Une durée de 0 garde la pop-up affichée jusqu'à fermeture manuelle
        if (duree > 0) {
            setTimeout(() => remove(id), duree);
        }
    }

    function success(message: string, duree?: number) {
        show("success", message, duree);
    }

    function error(message: string, duree?: number) {
        show("error", message, duree);
    }

    function remove(id: number) {
        notifications.value = notifications.value.filter((n) => n.id !== id);
    }

    return { notifications, show, success, error, remove };
});
