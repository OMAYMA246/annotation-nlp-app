package com.annotation.controller;
import com.annotation.model.Dataset;
import com.annotation.service.DatasetService;
import com.annotation.service.MLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/datasets/{id}/ml")
@RequiredArgsConstructor
public class AdminMLController {
    private final DatasetService datasetService;
    private final MLService mlService;

    @GetMapping
    public String pageMl(@PathVariable Long id, Model model) {
        Dataset dataset = datasetService.findById(id);
        model.addAttribute("dataset", dataset);
        model.addAttribute("historique", mlService.findByDataset(dataset));
        return "admin/datasets/ml";
    }

    @PostMapping("/entrainer")
    public String entrainer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            mlService.lancerEntrainement(datasetService.findById(id));
            ra.addFlashAttribute("success", "Entrainement lance ! Actualisez dans quelques secondes.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/datasets/" + id + "/ml";
    }

    @PostMapping("/predire")
    public String predire(@PathVariable Long id, @RequestParam String texte, RedirectAttributes ra) {
        try {
            ra.addFlashAttribute("predictionResult", mlService.predire(datasetService.findById(id), texte));
            ra.addFlashAttribute("textePredit", texte);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur prediction : " + e.getMessage());
        }
        return "redirect:/admin/datasets/" + id + "/ml";
    }
}
