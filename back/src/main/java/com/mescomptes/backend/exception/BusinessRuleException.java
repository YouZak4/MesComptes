package com.mescomptes.backend.exception;

// Demande valide sur la forme mais contraire à une règle métier (périodes non contiguës, etc.)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
