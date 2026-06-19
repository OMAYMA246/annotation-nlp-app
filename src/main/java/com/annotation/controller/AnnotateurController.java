package com.annotation.controller;

import com.annotation.model.Exemple;
import com.annotation.model.Tache;
import com.annotation.model.Utilisateur;
import com.annotation.repository.ExempleRepository;
import com.annotation.service.AnnotationService;
import com.annotation.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/annotateur")
@RequiredArgsConstructor
public class AnnotateurController {

    private final AnnotationService annotationService;
    private final UtilisateurService utilisateurService;
    private final ExempleRepository exempleRepository;

    private Utilisateur getConnecte(Authentication auth) {
        return utilisateurService.findByLogin(auth.getName());
    }

    // ---- Liste des tâches (regroupées par dataset, conforme UC6 de la maquette) ----
    @GetMapping("/taches")
    public String listeTaches(Authentication auth, Model model) {
        Utilisateur annotateur = getConnecte(auth);
        List<Tache> taches = annotationService.findTachesParAnnotateur(annotateur);
        model.addAttribute("taches", taches);
        model.addAttribute("tachesParDataset", annotationService.getTachesParDataset(annotateur));
        model.addAttribute("stats", annotationService.getStatsAnnotateur(annotateur));
        return "annotateur/taches";
    }

    // ---- Interface d'annotation ----
    @GetMapping("/annoter/{exempleId}")
    public String annoterPage(
            @PathVariable Long exempleId,
            Authentication auth,
            Model model) {

        Utilisateur annotateur = getConnecte(auth);
        Exemple exemple = exempleRepository.findById(exempleId)
            .orElseThrow(() -> new RuntimeException("Exemple introuvable"));

        // Vérifier que cet exemple lui est bien assigné
        boolean assigné = annotationService.findTachesParAnnotateur(annotateur).stream()
            .anyMatch(t -> t.getExemple().getId().equals(exempleId));
        if (!assigné) return "redirect:/annotateur/taches";

        // Annotation existante si déjà faite
        var annotationExistante = annotationService.findByDataset(exemple.getDataset()).stream()
            .filter(a -> a.getExemple().getId().equals(exempleId)
                && a.getAnnotateur().getId().equals(annotateur.getId()))
            .findFirst();

        model.addAttribute("exemple", exemple);
        model.addAttribute("dataset", exemple.getDataset());
        model.addAttribute("classesPossibles", exemple.getDataset().getClassesPossibles());
        model.addAttribute("annotationExistante", annotationExistante.orElse(null));

        // Navigation : trouver précédent / suivant parmi ses tâches non terminées
        List<Tache> taches = annotationService.findTachesNonTerminees(annotateur);
        int idx = -1;
        for (int i = 0; i < taches.size(); i++) {
            if (taches.get(i).getExemple().getId().equals(exempleId)) { idx = i; break; }
        }
        model.addAttribute("precedentId", idx > 0 ? taches.get(idx - 1).getExemple().getId() : null);
        model.addAttribute("suivantId", idx >= 0 && idx < taches.size() - 1 ? taches.get(idx + 1).getExemple().getId() : null);
        model.addAttribute("positionActuelle", idx + 1);
        model.addAttribute("totalTaches", taches.size());

        return "annotateur/annoter";
    }

        @PostMapping("/annoter/{exempleId}")
    public String sauvegarderAnnotation(
            @PathVariable Long exempleId,
            @RequestParam String classeChoisie,
            @RequestParam(defaultValue = "false") boolean suivant,
            Authentication auth,
            RedirectAttributes ra) {

        Utilisateur annotateur = getConnecte(auth);
        Exemple exemple = exempleRepository.findById(exempleId)
            .orElseThrow(() -> new RuntimeException("Exemple introuvable"));

        annotationService.annoter(exemple, annotateur, classeChoisie);

        if (suivant) {
            List<Tache> restantes = annotationService.findTachesNonTerminees(annotateur);
            if (!restantes.isEmpty()) {
                return "redirect:/annotateur/annoter/" + restantes.get(0).getExemple().getId();
            }
            ra.addFlashAttribute("success", "Toutes vos taches sont terminees !");
            return "redirect:/annotateur/taches";
        }
        ra.addFlashAttribute("success", "Annotation sauvegardee.");
        return "redirect:/annotateur/annoter/" + exempleId;
    }
}
