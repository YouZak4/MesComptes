package com.mescomptes.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void envoyerCodeVerification(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("MesComptes - votre code de vérification");
        message.setText("Votre code de vérification est : " + code
                + "\nIl expire dans 15 minutes.");
        mailSender.send(message);
    }

    public void envoyerAlerteCompteExistant(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("MesComptes - tentative d'inscription");
        message.setText("Quelqu'un a tenté de créer un compte MesComptes avec votre adresse e-mail,"
                + " mais un compte existe déjà pour cette adresse."
                + "\nSi c'est vous, connectez-vous simplement avec votre identifiant habituel."
                + "\nSi ce n'est pas vous, vous pouvez ignorer ce message : aucun compte n'a été créé.");
        mailSender.send(message);
    }

    public void envoyerCodeChangementEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("MesComptes - confirmez votre nouvelle adresse mail");
        message.setText("Votre code pour confirmer cette nouvelle adresse est : " + code
                + "\nIl expire dans 15 minutes."
                + "\nSi vous n'avez rien demandé, ignorez ce message : rien ne sera changé.");
        mailSender.send(message);
    }

    public void envoyerAlerteEmailDejaUtilise(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("MesComptes - tentative d'utilisation de votre adresse");
        message.setText("Quelqu'un a tenté d'associer votre adresse e-mail à un autre compte MesComptes,"
                + " mais elle est déjà liée à votre compte. Rien n'a été changé."
                + "\nVous pouvez ignorer ce message.");
        mailSender.send(message);
    }

    // Envoyé à l'ancienne adresse : si le changement n'est pas de son fait, le titulaire le sait
    public void envoyerAvisChangementEmail(String ancienEmail, String nouvelEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(ancienEmail);
        message.setSubject("MesComptes - votre adresse mail a été changée");
        message.setText("L'adresse mail de votre compte MesComptes est désormais " + nouvelEmail + "."
                + "\nSi vous n'êtes pas à l'origine de ce changement, changez votre mot de passe sans attendre.");
        mailSender.send(message);
    }
}
