package com.annotation.controller;

import com.annotation.service.AnnotationService;
import com.annotation.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/annotateurs")
@RequiredArgsConstructor
public class AdminAnnotateurController {

    private final UtilisateurService utilisateurService;
    private final AnnotationService annotationService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("annotateurs", utilisateurService.findAllAnnotateurs());
        return "admin/annotateurs/liste";
    }

    @GetMapping("/nouveau")
    public String formNouveau() {
        return "admin/annotateurs/form";
    }

    @PostMapping("/nouveau")
    public String creer(
            @RequestParam String nom,
            @RequestParam String prenom,
            RedirectAttributes ra) {
        try {
            var resultat = utilisateurService.creerAnnotateur(nom, prenom);
            ra.addFlashAttribute("success",
                "Annotateur créé avec succès ! Identifiants — Login : " + resultat.utilisateur().getLogin()
                    + " / Mot de passe : " + resultat.motDePasseClair()
                    + " (notez-le, il ne sera plus affiché).");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/annotateurs";
    }

    @GetMapping("/{id}/modifier")
    public String formModifier(@PathVariable Long id, Model model) {
        model.addAttribute("annotateur", utilisateurService.findById(id));
        return "admin/annotateurs/modifier";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(
            @PathVariable Long id,
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String login,
            @RequestParam(required = false) String password,
            RedirectAttributes ra) {
        try {
            utilisateurService.modifierAnnotateur(id, nom, prenom, login, password);
            ra.addFlashAttribute("success", "Annotateur modifié.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/annotateurs";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        utilisateurService.supprimerAnnotateur(id);
        ra.addFlashAttribute("success", "Annotateur supprimé.");
        return "redirect:/admin/annotateurs";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        utilisateurService.toggleActive(id);
        ra.addFlashAttribute("success", "Statut mis à jour.");
        return "redirect:/admin/annotateurs";
    }

    @GetMapping("/{id}/stats")
    public String stats(@PathVariable Long id, Model model) {
        var annotateur = utilisateurService.findById(id);
        model.addAttribute("annotateur", annotateur);
        model.addAttribute("stats", annotationService.getStatsAnnotateur(annotateur));
        return "admin/annotateurs/stats";
    }
}

