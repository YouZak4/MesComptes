package com.mescomptes.backend.config;

import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Libelle;
import com.mescomptes.backend.model.enums.TypeOperation;
import com.mescomptes.backend.repository.CategorieRepository;
import com.mescomptes.backend.repository.LibelleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mescomptes.backend.model.enums.TypeOperation.DEPENSE;
import static com.mescomptes.backend.model.enums.TypeOperation.REVENU;

/**
 * Insère au démarrage la liste de catégories et libellés fournie à tous les utilisateurs.
 * Seuls les éléments absents sont ajoutés : on peut relancer l'application ou compléter la liste sans doublon.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonneesFourniesInitializer implements ApplicationRunner {

    private record CategorieFournie(String nom, TypeOperation type, String icone, String couleur, List<String> libelles) {
    }

    private static final List<CategorieFournie> CATEGORIES = List.of(
            new CategorieFournie("Logement", DEPENSE, "house", "#8A5A44", List.of("Loyer", "Électricité", "Gaz", "Eau", "Taxe d'habitation")),
            new CategorieFournie("Courses", DEPENSE, "shopping-cart", "#9A6410", List.of("Supermarché", "Boulangerie", "Marché", "Boucherie")),
            new CategorieFournie("Sorties & restaurants", DEPENSE, "utensils", "#A86400", List.of("Restaurant", "Fast-food", "Café", "Bar")),
            new CategorieFournie("Abonnements & loisirs", DEPENSE, "tv", "#3F51A3", List.of("Netflix", "Deezer", "Spotify", "Cinéma", "Jeux vidéo")),
            new CategorieFournie("Téléphone & internet", DEPENSE, "smartphone", "#1F64B5", List.of("Forfait mobile", "Box internet")),
            new CategorieFournie("Assurances", DEPENSE, "shield-check", "#4A6378", List.of("Assurance auto", "Assurance moto", "Assurance habitation", "Prévoyance", "Assurance vie")),
            new CategorieFournie("Santé", DEPENSE, "heart-pulse", "#B83227", List.of("Médecin", "Pharmacie", "Opticien", "Dentiste", "Mutuelle")),
            new CategorieFournie("Soins & beauté", DEPENSE, "sparkles", "#B23A72", List.of("Coiffeur", "Cosmétiques")),
            new CategorieFournie("Carburant", DEPENSE, "fuel", "#B8501A", List.of("Essence voiture", "Essence moto")),
            new CategorieFournie("Véhicule", DEPENSE, "car", "#34449A", List.of("Entretien", "Révision", "Péage", "Parking", "Équipement moto", "Permis")),
            new CategorieFournie("Prêts", DEPENSE, "percent", "#6B5240", List.of("Prêt auto", "Prêt moto", "Crédit immobilier")),
            new CategorieFournie("Banque", DEPENSE, "landmark", "#4F5A66", List.of("Cotisation carte", "Frais bancaires")),
            new CategorieFournie("Sport", DEPENSE, "dumbbell", "#0F7A70", List.of("Salle de sport", "Compléments alimentaires", "Inscription course")),
            new CategorieFournie("Vacances & voyages", DEPENSE, "plane", "#0A779F", List.of("Train", "Avion", "Hôtel", "Location")),
            new CategorieFournie("Maison & entretien", DEPENSE, "paint-roller", "#6B7A1E", List.of("Laverie", "Bricolage", "Ménage")),
            new CategorieFournie("Cadeaux", DEPENSE, "gift", "#A8335F", List.of("Cadeau anniversaire", "Cadeau Noël")),
            new CategorieFournie("Autres dépenses", DEPENSE, "package", "#6B7079", List.of()),
            new CategorieFournie("Salaire", REVENU, "briefcase", "#1B5C42", List.of("Salaire", "Prime")),
            new CategorieFournie("Aides", REVENU, "handshake", "#1B5C42", List.of("CAF", "Aide au logement")),
            new CategorieFournie("Remboursements", REVENU, "undo-2", "#1B5C42", List.of("Remboursement mutuelle", "Remboursement d'un proche", "Covoiturage")),
            new CategorieFournie("Autres revenus", REVENU, "piggy-bank", "#1B5C42", List.of())
    );

    private final CategorieRepository categorieRepository;
    private final LibelleRepository libelleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Categorie> categories = categorieRepository.findFournies().stream()
                .collect(Collectors.toMap(Categorie::getNomNormalise, Function.identity()));
        Map<String, Libelle> libelles = libelleRepository.findFournis().stream()
                .collect(Collectors.toMap(Libelle::getNomNormalise, Function.identity()));
        int ajouts = 0;

        for (CategorieFournie fournie : CATEGORIES) {
            Categorie categorie = categories.get(Noms.normaliser(fournie.nom()));
            if (categorie == null) {
                categorie = new Categorie();
                categorie.setNom(fournie.nom());
                categorie.setType(fournie.type());
                categorie.setIcone(fournie.icone());
                categorie.setCouleur(fournie.couleur());
                categorie = categorieRepository.save(categorie);
                ajouts++;
            }
            for (String nomLibelle : fournie.libelles()) {
                if (!libelles.containsKey(Noms.normaliser(nomLibelle))) {
                    Libelle libelle = new Libelle();
                    libelle.setNom(nomLibelle);
                    libelle.setCategorieParDefaut(categorie);
                    libelles.put(Noms.normaliser(nomLibelle), libelleRepository.save(libelle));
                    ajouts++;
                }
            }
        }
        if (ajouts > 0) log.info("Données fournies : {} catégories et libellés ajoutés", ajouts);
    }
}
