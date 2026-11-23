package com.mescomptes.backend.model.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Taille maximale d'un texte une fois encodé en UTF-8, en octets (et non en caractères comme {@code @Size}).
 * Une valeur nulle est acceptée : {@code @NotBlank} s'en charge.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OctetsMax.Validateur.class)
public @interface OctetsMax {

    int value();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validateur implements ConstraintValidator<OctetsMax, String> {

        private int max;

        @Override
        public void initialize(OctetsMax annotation) {
            max = annotation.value();
        }

        @Override
        public boolean isValid(String valeur, ConstraintValidatorContext contexte) {
            return valeur == null || valeur.getBytes(StandardCharsets.UTF_8).length <= max;
        }
    }
}
