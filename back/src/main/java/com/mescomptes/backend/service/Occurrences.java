package com.mescomptes.backend.service;

import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.model.enums.Frequence;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Calendrier d'une récurrence. Un jour absent du mois (31 en avril, 29 février) devient le dernier jour du mois.
 * Une récurrence annuelle tombe chaque année dans le mois de sa date de début.
 */
public final class Occurrences {

    private Occurrences() {
    }

    /**
     * Première occurrence à partir d'une date (incluse).
     */
    public static LocalDate premiereAPartirDe(Recurrence recurrence, LocalDate date) {
        YearMonth mois = recurrence.getFrequence() == Frequence.MENSUELLE
                ? YearMonth.from(date)
                : YearMonth.of(date.getYear(), recurrence.getDateDebut().getMonth());
        LocalDate candidate = jourDansLeMois(mois, recurrence.getJour());
        return candidate.isBefore(date) ? suivante(recurrence, candidate) : candidate;
    }

    /**
     * Occurrence qui suit une occurrence donnée.
     */
    public static LocalDate suivante(Recurrence recurrence, LocalDate occurrence) {
        YearMonth mois = YearMonth.from(occurrence);
        mois = recurrence.getFrequence() == Frequence.MENSUELLE ? mois.plusMonths(1) : mois.plusYears(1);
        return jourDansLeMois(mois, recurrence.getJour());
    }

    /**
     * Occurrences pas encore générées (à partir de la prochaine occurrence) comprises entre deux dates incluses,
     * en s'arrêtant à la date de fin de la récurrence.
     */
    public static List<LocalDate> aGenererEntre(Recurrence recurrence, LocalDate debut, LocalDate fin) {
        LocalDate limite = recurrence.getDateFin() != null && recurrence.getDateFin().isBefore(fin) ? recurrence.getDateFin() : fin;
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = recurrence.getProchaineOccurrence();
        while (!date.isAfter(limite)) {
            if (!date.isBefore(debut)) dates.add(date);
            date = suivante(recurrence, date);
        }
        return dates;
    }

    private static LocalDate jourDansLeMois(YearMonth mois, int jour) {
        return mois.atDay(Math.min(jour, mois.lengthOfMonth()));
    }
}
