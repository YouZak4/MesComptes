# 0005 – Un mot de passe actuel faux répond 422, pas 401

- **Date :** 2026-10-13
- **Statut :** Accepté
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation.

## Contexte

Pour changer son mot de passe ou son adresse mail, l'utilisateur connecté saisit son mot de passe actuel. S'il se trompe, la réponse naturelle serait 401 (non authentifié).

Or l'intercepteur du front (`services/api.ts`) déconnecte l'utilisateur sur toute réponse 401, car un 401 signifie normalement que le jeton est absent ou expiré. Une simple faute de frappe fermerait la session.

## Décision

Un mot de passe actuel incorrect répond **422** `BUSINESS_RULE_VIOLATION`, avec un message en français affichable tel quel. Le 401 reste réservé aux problèmes de jeton et à l'échec de connexion (`POST /auth/login`).

## Conséquences

- L'utilisateur reste connecté et voit le message d'erreur dans le formulaire.
- Personne ne doit « corriger » ce 422 en 401 : ce serait un bug côté front.
- Toute nouvelle route qui revérifie le mot de passe d'un utilisateur connecté suit la même règle.
