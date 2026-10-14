package com.mescomptes.backend.service;

import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.model.enums.Frequence;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OccurrencesTests {

    @Test
    void unJourAbsentDuMoisDevientLeDernierJourSansDecalerLesMoisSuivants() {
        Recurrence loyer = recurrence(Frequence.MENSUELLE, 31, "2026-01-31");

        assertThat(Occurrences.suivante(loyer, d("2026-01-31"))).isEqualTo("2026-02-28");
        assertThat(Occurrences.suivante(loyer, d("2026-02-28"))).isEqualTo("2026-03-31");
        assertThat(Occurrences.suivante(loyer, d("2026-03-31"))).isEqualTo("2026-04-30");
    }

    @Test
    void uneRecurrenceAnnuelleDu29FevrierTombeLe28LesAnneesNonBissextiles() {
        Recurrence assurance = recurrence(Frequence.ANNUELLE, 29, "2024-02-29");

        assertThat(Occurrences.suivante(assurance, d("2024-02-29"))).isEqualTo("2025-02-28");
        assertThat(Occurrences.suivante(assurance, d("2027-02-28"))).isEqualTo("2028-02-29");
    }

    @Test
    void premiereOccurrenceAPartirDUneDate() {
        Recurrence mensuelle = recurrence(Frequence.MENSUELLE, 5, "2026-01-05");
        Recurrence annuelle = recurrence(Frequence.ANNUELLE, 15, "2025-06-15");

        assertThat(Occurrences.premiereAPartirDe(mensuelle, d("2026-03-05"))).isEqualTo("2026-03-05");
        assertThat(Occurrences.premiereAPartirDe(mensuelle, d("2026-03-10"))).isEqualTo("2026-04-05");
        assertThat(Occurrences.premiereAPartirDe(annuelle, d("2026-01-01"))).isEqualTo("2026-06-15");
        assertThat(Occurrences.premiereAPartirDe(annuelle, d("2026-07-01"))).isEqualTo("2027-06-15");
    }

    @Test
    void occurrencesAGenererDansUnIntervalleEnRespectantLaDateDeFin() {
        Recurrence pret = recurrence(Frequence.MENSUELLE, 10, "2026-01-10");
        pret.setProchaineOccurrence(d("2026-02-10"));

        assertThat(Occurrences.aGenererEntre(pret, d("2026-01-01"), d("2026-04-30")))
                .containsExactly(d("2026-02-10"), d("2026-03-10"), d("2026-04-10"));

        pret.setDateFin(d("2026-03-31"));
        assertThat(Occurrences.aGenererEntre(pret, d("2026-03-01"), d("2026-04-30"))).containsExactly(d("2026-03-10"));
    }

    private static Recurrence recurrence(Frequence frequence, int jour, String debut) {
        Recurrence recurrence = new Recurrence();
        recurrence.setFrequence(frequence);
        recurrence.setJour(jour);
        recurrence.setDateDebut(d(debut));
        recurrence.setProchaineOccurrence(d(debut));
        return recurrence;
    }

    private static LocalDate d(String date) {
        return LocalDate.parse(date);
    }
}
