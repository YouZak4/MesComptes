package com.mescomptes.backend.exception;

// Ressource absente ou appartenant à un autre utilisateur : dans les deux cas 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
