package com.annotation.service;

import com.annotation.model.*;
import com.annotation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final TacheRepository tacheRepository;
    private final ExempleRepository exempleRepository;

    /**
     * Sauvegarde ou met à jour une annotation et marque la tâche comme terminée.
     */
    @Transactional
    public Annotation annoter(Exemple exemple, Utilisateur annotateur, String classeChoisie) {
        Annotation annotation = annotationRepository
            .findByExempleAndAnnotateur(exemple, annotateur)
            .orElse(Annotation.builder()
                .exemple(exemple)
                .annotateur(annotateur)
                .build());

        annotation.setClasseChoisie(classeChoisie);
        annotation = annotationRepository.save(annotation);

        // Marquer la tâche correspondante comme terminée
        tacheRepository.findByAnnotateur(annotateur).stream()
            .filter(t -> t.getExemple().getId().equals(exemple.getId()))
            .forEach(t -> {
                t.setTerminee(true);
                tacheRepository.save(t);
            });

        return annotation;
    }

    public List<Annotation> findByDataset(Dataset dataset) {
        return annotationRepository.findByDataset(dataset);
    }

    public List<Tache> findTachesParAnnotateur(Utilisateur annotateur) {
        return tacheRepository.findByAnnotateur(annotateur);
    }

    public List<Tache> findTachesNonTerminees(Utilisateur annotateur) {
        return tacheRepository.findByAnnotateurAndTerminee(annotateur, false);
    }

    /**
     * Calcul du Cohen's Kappa pour un dataset.
     * Simplifié pour 2 annotateurs; pour N annotateurs → Fleiss Kappa.
     */
    public double calculerKappa(Dataset dataset) {
        List<Annotation> annotations = annotationRepository.findByDataset(dataset);
        if (annotations.isEmpty()) return 0.0;

        // Grouper par exemple
        Map<Long, List<String>> parExemple = annotations.stream()
            .collect(Collectors.groupingBy(
                a -> a.getExemple().getId(),
                Collectors.mapping(Annotation::getClasseChoisie, Collectors.toList())
            ));

        // Garder seulement les exemples annotés par au moins 2 annotateurs
        List<Map.Entry<Long, List<String>>> doubles = parExemple.entrySet().stream()
            .filter(e -> e.getValue().size() >= 2)
            .collect(Collectors.toList());

        if (doubles.isEmpty()) return 0.0;

        // Fleiss Kappa simplifié
        long total = doubles.size();
        long accords = doubles.stream()
            .filter(e -> e.getValue().stream().distinct().count() == 1)
            .count();

        double po = (double) accords / total; // accord observé

        // Distribution marginale des classes
        Map<String, Long> classCounts = annotations.stream()
            .collect(Collectors.groupingBy(Annotation::getClasseChoisie, Collectors.counting()));
        long totalAnnotations = annotations.size();

        double pe = classCounts.values().stream()
            .mapToDouble(c -> Math.pow((double) c / totalAnnotations, 2))
            .sum();

        if (pe >= 1.0) return 1.0;
        return (po - pe) / (1.0 - pe);
    }

    /**
     * Détecte les potentiels spammeurs : annotateurs dont la distribution
     * de classes est trop uniforme (entropie proche du max).
     */
    public List<Utilisateur> detecterSpammeurs(Dataset dataset) {
        List<Annotation> annotations = annotationRepository.findByDataset(dataset);

        Map<Utilisateur, List<Annotation>> parAnnotateur = annotations.stream()
            .collect(Collectors.groupingBy(Annotation::getAnnotateur));

        List<Utilisateur> suspects = new ArrayList<>();
        for (Map.Entry<Utilisateur, List<Annotation>> entry : parAnnotateur.entrySet()) {
            List<Annotation> annots = entry.getValue();
            if (annots.size() < 10) continue; // pas assez d'annotations

            Map<String, Long> dist = annots.stream()
                .collect(Collectors.groupingBy(Annotation::getClasseChoisie, Collectors.counting()));

            // Calculer entropie normalisée
            int nbClasses = dist.size();
            if (nbClasses <= 1) continue;

            double entropie = 0;
            for (long count : dist.values()) {
                double p = (double) count / annots.size();
                if (p > 0) entropie -= p * Math.log(p) / Math.log(2);
            }
            double entropieMax = Math.log(nbClasses) / Math.log(2);
            double entropieNorm = entropieMax > 0 ? entropie / entropieMax : 0;

            // Si entropie normalisée > 0.95 → distribution trop uniforme → suspect
            if (entropieNorm > 0.95) {
                suspects.add(entry.getKey());
            }
        }
        return suspects;
    }

    /**
     * Statistiques globales pour le tableau de bord admin.
     */
    public Map<String, Object> getStatsDataset(Dataset dataset) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Annotation> annotations = annotationRepository.findByDataset(dataset);
        long totalExemples = exempleRepository.countByDataset(dataset);
        long totalAnnotations = annotations.size();
        long tachesTerminees = tacheRepository.countByDatasetAndTerminee(dataset, true);
        long totalTaches = tacheRepository.findByDataset(dataset).size();

        stats.put("totalExemples", totalExemples);
        stats.put("totalAnnotations", totalAnnotations);
        stats.put("tachesTerminees", tachesTerminees);
        stats.put("totalTaches", totalTaches);
        stats.put("progression", totalTaches > 0 ? (double) tachesTerminees / totalTaches * 100 : 0);
        stats.put("kappa", calculerKappa(dataset));

        // Distribution des classes
        Map<String, Long> distribution = annotations.stream()
            .collect(Collectors.groupingBy(Annotation::getClasseChoisie, Collectors.counting()));
        stats.put("distribution", distribution);

        return stats;
    }

    /**
     * Stats personnelles pour un annotateur.
     */
    public Map<String, Object> getStatsAnnotateur(Utilisateur annotateur) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Tache> taches = tacheRepository.findByAnnotateur(annotateur);
        long terminées = taches.stream().filter(Tache::isTerminee).count();

        stats.put("totalTaches", taches.size());
        stats.put("tachesTerminees", terminées);
        stats.put("tachesRestantes", taches.size() - terminées);
        stats.put("progression", taches.size() > 0 ? (double) terminées / taches.size() * 100 : 0);

        List<Annotation> annotations = annotationRepository.findByAnnotateur(annotateur);
        Map<String, Long> distribution = annotations.stream()
            .collect(Collectors.groupingBy(Annotation::getClasseChoisie, Collectors.counting()));
        stats.put("distribution", distribution);

        return stats;
    }
}
