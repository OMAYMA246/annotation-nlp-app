package com.annotation.service;
import com.annotation.model.*;
import com.annotation.repository.ModelEntrainementRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.file.*;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class MLService {
    private final ModelEntrainementRepository modelRepo;
    private final ExportService exportService;
    private final ObjectMapper objectMapper;
    private static final String WORK_DIR  = System.getProperty("user.dir") + "/ml/";
    private static final String MODEL_DIR = System.getProperty("user.dir") + "/ml/models/";

    public List<ModelEntrainement> findByDataset(Dataset dataset) {
        return modelRepo.findByDatasetOrderByDateEntrainementDesc(dataset);
    }

    @Transactional
    public ModelEntrainement lancerEntrainement(Dataset dataset) throws Exception {
        ModelEntrainement model = ModelEntrainement.builder()
            .dataset(dataset).statut("EN_COURS").build();
        model = modelRepo.save(model);
        final Long modelId = model.getId();
        Files.createDirectories(Paths.get(MODEL_DIR));
        String annotationsPath = WORK_DIR + "annotations_" + dataset.getId() + ".json";
        Files.write(Paths.get(annotationsPath), exportService.exporterJSON(dataset));
        final String datasetId = String.valueOf(dataset.getId());
        Thread thread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "python", WORK_DIR + "train.py",
                    "--input", annotationsPath,
                    "--output", MODEL_DIR,
                    "--dataset-id", datasetId);
                pb.redirectErrorStream(true);
                pb.directory(new File(WORK_DIR));
                Process process = pb.start();
                StringBuilder logs = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) { logs.append(line).append("\n"); log.info("[ML] {}", line); }
                }
                int exitCode = process.waitFor();
                String resultPath = MODEL_DIR + "result_" + datasetId + ".json";
                if (Files.exists(Paths.get(resultPath))) {
                    JsonNode result = objectMapper.readTree(new String(Files.readAllBytes(Paths.get(resultPath))));
                    updateModel(modelId, result, logs.toString(), exitCode == 0);
                } else {
                    updateModelErreur(modelId, "Fichier de resultats introuvable", logs.toString());
                }
            } catch (Exception e) { updateModelErreur(modelId, e.getMessage(), ""); }
        });
        thread.setDaemon(true);
        thread.start();
        return model;
    }

    @Transactional
    public void updateModel(Long modelId, JsonNode result, String logs, boolean success) {
        modelRepo.findById(modelId).ifPresent(m -> {
            if (success && result.has("metrics")) {
                JsonNode mx = result.get("metrics");
                m.setStatut("SUCCES");
                m.setAccuracy(mx.has("accuracy") ? mx.get("accuracy").asDouble() : null);
                m.setF1Score(mx.has("f1_score") ? mx.get("f1_score").asDouble() : null);
                m.setTrainSize(mx.has("train_size") ? mx.get("train_size").asInt() : null);
                m.setTestSize(mx.has("test_size") ? mx.get("test_size").asInt() : null);
                m.setClassesJson(mx.has("classes") ? mx.get("classes").toString() : null);
                m.setConfusionMatrixJson(mx.has("confusion_matrix") ? mx.get("confusion_matrix").toString() : null);
                m.setRapportJson(mx.has("classification_report") ? mx.get("classification_report").toString() : null);
            } else {
                m.setStatut("ERREUR");
                m.setMessageErreur(result.has("error") ? result.get("error").asText() : "Erreur inconnue");
            }
            m.setLogsEntrainement(logs);
            modelRepo.save(m);
        });
    }

    @Transactional
    public void updateModelErreur(Long modelId, String erreur, String logs) {
        modelRepo.findById(modelId).ifPresent(m -> {
            m.setStatut("ERREUR"); m.setMessageErreur(erreur); m.setLogsEntrainement(logs);
            modelRepo.save(m);
        });
    }

    public String predire(Dataset dataset, String texte) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "python", WORK_DIR + "predict.py",
            "--model", MODEL_DIR,
            "--dataset-id", String.valueOf(dataset.getId()),
            "--text", texte);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) output.append(line);
        }
        process.waitFor();
        return output.toString();
    }
}
