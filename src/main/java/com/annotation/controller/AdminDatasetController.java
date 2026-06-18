package com.annotation.controller;

import com.annotation.model.Dataset;
import com.annotation.service.AnnotationService;
import com.annotation.service.DatasetService;
import com.annotation.service.ExportService;
import com.annotation.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDatasetController {

    private final DatasetService datasetService;
    private final UtilisateurService utilisateurService;
    private final AnnotationService annotationService;
    private final ExportService exportService;

    // ---- Liste des datasets ----
    @GetMapping("/datasets")
    public String listeDatasets(Model model) {
        model.addAttribute("datasets", datasetService.findAll());
        return "admin/datasets/liste";
    }

    // ---- Formulaire création dataset ----
    @GetMapping("/datasets/nouveau")
    public String formNouveauDataset() {
        return "admin/datasets/creer";
    }

    @PostMapping("/datasets/nouveau")
    public String creerDataset(
            @RequestParam String nom,
            @RequestParam(required = false) String description,
            @RequestParam String classes,
            @RequestParam int nombreTextesParExemple,
            @RequestParam MultipartFile fichier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateLimite,
            RedirectAttributes ra) {
        try {
            datasetService.creerDataset(nom, description, classes, nombreTextesParExemple, fichier, dateLimite);
            ra.addFlashAttribute("success", "Dataset créé avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la création : " + e.getMessage());
        }
        return "redirect:/admin/datasets";
    }

    // ---- Détails d'un dataset ----
    @GetMapping("/datasets/{id}")
    public String detailsDataset(@PathVariable Long id, Model model) {
        Dataset dataset = datasetService.findById(id);
        model.addAttribute("dataset", dataset);
        model.addAttribute("stats", annotationService.getStatsDataset(dataset));
        model.addAttribute("spammeurs", annotationService.detecterSpammeurs(dataset));
        return "admin/datasets/details";
    }

    // ---- Affectation des annotateurs ----
    @GetMapping("/datasets/{id}/affecter")
    public String formAffecter(@PathVariable Long id, Model model) {
        model.addAttribute("dataset", datasetService.findById(id));
        model.addAttribute("tousAnnotateurs", utilisateurService.findAllAnnotateurs());
        return "admin/datasets/affecter";
    }

    @PostMapping("/datasets/{id}/affecter")
    public String affecterAnnotateurs(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> annotateurIds,
            RedirectAttributes ra) {
        if (annotateurIds == null || annotateurIds.isEmpty()) {
            ra.addFlashAttribute("error", "Veuillez sélectionner au moins un annotateur.");
            return "redirect:/admin/datasets/" + id + "/affecter";
        }
        datasetService.affecterAnnotateurs(id, annotateurIds);
        ra.addFlashAttribute("success", "Annotateurs affectés et tâches distribuées !");
        return "redirect:/admin/datasets/" + id;
    }

    @PostMapping("/datasets/{datasetId}/desaffecter/{annotateurId}")
    public String desaffecterAnnotateur(
            @PathVariable Long datasetId,
            @PathVariable Long annotateurId,
            RedirectAttributes ra) {
        datasetService.desaffecterAnnotateur(datasetId, annotateurId);
        ra.addFlashAttribute("success", "Annotateur désaffecté (travail conservé).");
        return "redirect:/admin/datasets/" + datasetId;
    }

    @PostMapping("/datasets/{id}/supprimer")
    public String supprimerDataset(@PathVariable Long id, RedirectAttributes ra) {
        datasetService.supprimerDataset(id);
        ra.addFlashAttribute("success", "Dataset supprimé.");
        return "redirect:/admin/datasets";
    }

    // ---- Export ----
    @GetMapping("/datasets/{id}/export/csv")
    public ResponseEntity<byte[]> exportCSV(@PathVariable Long id) throws Exception {
        Dataset dataset = datasetService.findById(id);
        byte[] data = exportService.exporterCSV(dataset);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"annotations_" + id + ".csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(data);
    }

    @GetMapping("/datasets/{id}/export/json")
    public ResponseEntity<byte[]> exportJSON(@PathVariable Long id) throws Exception {
        Dataset dataset = datasetService.findById(id);
        byte[] data = exportService.exporterJSON(dataset);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"annotations_" + id + ".json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(data);
    }
}