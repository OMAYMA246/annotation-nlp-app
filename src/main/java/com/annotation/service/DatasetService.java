package com.annotation.service;

import com.annotation.model.*;
import com.annotation.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final ExempleRepository exempleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TacheRepository tacheRepository;
    private final ObjectMapper objectMapper;

    public List<Dataset> findAll() {
        return datasetRepository.findAll();
    }

    public Dataset findById(Long id) {
        return datasetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dataset introuvable: " + id));
    }

    /**
     * Crée un dataset en important un fichier CSV ou JSON.
     * Le fichier CSV doit avoir des colonnes texte1 (et texte2 optionnel).
     * Le fichier JSON doit être un tableau d'objets avec champs textes[].
     */
    @Transactional
    public Dataset creerDataset(String nom, String description, String classesStr,
                                 int nombreTextesParExemple, MultipartFile fichier) throws Exception {
        Dataset dataset = Dataset.builder()
            .nomDataset(nom)
            .description(description)
            .nombreTextesParExemple(nombreTextesParExemple)
            .build();

        // Ajouter les classes possibles
        if (classesStr != null && !classesStr.isBlank()) {
            String[] classes = classesStr.split(";");
            for (String c : classes) {
                if (!c.isBlank()) {
                    ClassePossible cp = ClassePossible.builder()
                        .libelleClasse(c.trim())
                        .dataset(dataset)
                        .build();
                    dataset.getClassesPossibles().add(cp);
                }
            }
        }

        dataset = datasetRepository.save(dataset);

        // Importer les exemples depuis le fichier
        if (fichier != null && !fichier.isEmpty()) {
            String filename = fichier.getOriginalFilename() != null ? fichier.getOriginalFilename() : "";
            if (filename.endsWith(".csv")) {
                importerCSV(fichier, dataset, nombreTextesParExemple);
            } else if (filename.endsWith(".json")) {
                importerJSON(fichier, dataset, nombreTextesParExemple);
            } else {
                throw new RuntimeException("Format de fichier non supporté. Utilisez CSV ou JSON.");
            }
        }

        return dataset;
    }

    private void importerCSV(MultipartFile fichier, Dataset dataset, int nbTextes) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext(); // lire l'en-tête
            if (headers == null) return;

            // Trouver les indices des colonnes de texte
            List<Integer> textIndices = new ArrayList<>();
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                if (h.startsWith("text") || h.startsWith("texte") || h.equals("premise")
                    || h.equals("hypothesis") || h.equals("sentence1") || h.equals("sentence2")) {
                    textIndices.add(i);
                }
            }
            // Fallback: prendre les premières colonnes
            if (textIndices.isEmpty()) {
                for (int i = 0; i < Math.min(nbTextes, headers.length); i++) {
                    textIndices.add(i);
                }
            }

            String[] line;
            List<Exemple> exemples = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                List<String> textes = new ArrayList<>();
                for (int idx : textIndices) {
                    if (idx < line.length) {
                        textes.add(line[idx].trim());
                    }
                }
                if (!textes.isEmpty() && !textes.get(0).isBlank()) {
                    Exemple ex = Exemple.builder()
                        .textes(textes)
                        .dataset(dataset)
                        .build();
                    exemples.add(ex);
                }
            }
            exempleRepository.saveAll(exemples);
            log.info("Importé {} exemples depuis CSV", exemples.size());
        }
    }

    private void importerJSON(MultipartFile fichier, Dataset dataset, int nbTextes) throws Exception {
        JsonNode root = objectMapper.readTree(fichier.getInputStream());
        List<Exemple> exemples = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                List<String> textes = new ArrayList<>();
                // Chercher champ "textes" tableau
                if (node.has("textes") && node.get("textes").isArray()) {
                    for (JsonNode t : node.get("textes")) {
                        textes.add(t.asText());
                    }
                } else {
                    // Fallback: chercher text1/text2 ou premise/hypothesis
                    String[] candidats = {"text", "text1", "texte", "texte1", "premise", "sentence1", "text2", "texte2", "hypothesis", "sentence2"};
                    for (String c : candidats) {
                        if (node.has(c)) textes.add(node.get(c).asText());
                        if (textes.size() >= nbTextes) break;
                    }
                }
                if (!textes.isEmpty()) {
                    Exemple ex = Exemple.builder()
                        .textes(textes)
                        .dataset(dataset)
                        .build();
                    exemples.add(ex);
                }
            }
        }
        exempleRepository.saveAll(exemples);
        log.info("Importé {} exemples depuis JSON", exemples.size());
    }

    /**
     * Affecte des annotateurs au dataset et distribue les exemples aléatoirement.
     * Chaque exemple sera assigné à 3 annotateurs minimum (ou tous si moins de 3).
     */
    @Transactional
    public void affecterAnnotateurs(Long datasetId, List<Long> annotateurIds) {
        Dataset dataset = findById(datasetId);
        List<Exemple> exemples = exempleRepository.findByDataset(dataset);

        // Récupérer les annotateurs sélectionnés
        List<Utilisateur> annotateurs = utilisateurRepository.findAllById(annotateurIds);

        // Ajouter les annotateurs au dataset
        dataset.getAnnotateurs().addAll(annotateurs);
        datasetRepository.save(dataset);

        // Supprimer les anciennes tâches non terminées de ces annotateurs
        List<Tache> anciennesTaches = tacheRepository.findByDataset(dataset);
        tacheRepository.deleteAll(anciennesTaches);

        // Répartition : chaque exemple assigné à au moins 3 annotateurs
        int nbAnnotateurs = annotateurs.size();
        if (nbAnnotateurs == 0 || exemples.isEmpty()) return;

        int nbParAnnotateur = (int) Math.ceil((double) exemples.size() / nbAnnotateurs);
        // Mélanger les exemples
        List<Exemple> shuffled = new ArrayList<>(exemples);
        Collections.shuffle(shuffled);

        List<Tache> taches = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            // Assigner au moins 3 annotateurs par exemple (ou tous si < 3)
            int debut = i % nbAnnotateurs;
            int count = Math.min(3, nbAnnotateurs);
            for (int j = 0; j < count; j++) {
                Utilisateur annot = annotateurs.get((debut + j) % nbAnnotateurs);
                Tache tache = Tache.builder()
                    .exemple(shuffled.get(i))
                    .annotateur(annot)
                    .dataset(dataset)
                    .terminee(false)
                    .build();
                taches.add(tache);
            }
        }
        tacheRepository.saveAll(taches);
        log.info("Créé {} tâches pour {} annotateurs sur {} exemples",
            taches.size(), nbAnnotateurs, exemples.size());
    }

    @Transactional
    public void desaffecterAnnotateur(Long datasetId, Long annotateurId) {
        Dataset dataset = findById(datasetId);
        dataset.getAnnotateurs().removeIf(a -> a.getId().equals(annotateurId));
        datasetRepository.save(dataset);
        // Les annotations déjà faites sont conservées
    }

    @Transactional
    public void supprimerDataset(Long id) {
        datasetRepository.deleteById(id);
    }
}
