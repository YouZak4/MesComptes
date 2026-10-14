package com.mescomptes.backend.model.dto;

/**
 * Filtres facultatifs et combinables de la liste des opérations (paramètres de GET /operations).
 *
 * @param q texte cherché dans la note et le libellé
 */
public record FiltreOperations(Long periodeId, Long compteId, Long categorieId, Long libelleId, String q) {
}
