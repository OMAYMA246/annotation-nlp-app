package com.annotation.service;

import com.annotation.model.Role;
import com.annotation.model.Utilisateur;
import com.annotation.repository.RoleRepository;
import com.annotation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Utilisateur> findAllAnnotateurs() {
        return utilisateurRepository.findAllAnnotateurs();
    }

    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
    }

    public Utilisateur findByLogin(String login) {
        return utilisateurRepository.findByLogin(login)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + login));
    }

    @Transactional
    public Utilisateur creerAnnotateur(String nom, String prenom, String login, String password) {
        if (utilisateurRepository.existsByLogin(login)) {
            throw new RuntimeException("Login déjà utilisé: " + login);
        }
        Role role = roleRepository.findByNomRole("ANNOTATOR_ROLE")
            .orElseThrow(() -> new RuntimeException("Rôle ANNOTATOR_ROLE introuvable"));

        Utilisateur u = Utilisateur.builder()
            .nom(nom)
            .prenom(prenom)
            .login(login)
            .password(passwordEncoder.encode(password))
            .active(true)
            .roles(Set.of(role))
            .build();
        return utilisateurRepository.save(u);
    }

    @Transactional
    public Utilisateur modifierAnnotateur(Long id, String nom, String prenom, String login, String password) {
        Utilisateur u = findById(id);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setLogin(login);
        if (password != null && !password.isBlank()) {
            u.setPassword(passwordEncoder.encode(password));
        }
        return utilisateurRepository.save(u);
    }

    @Transactional
    public void supprimerAnnotateur(Long id) {
        utilisateurRepository.deleteById(id);
    }

    @Transactional
    public void toggleActive(Long id) {
        Utilisateur u = findById(id);
        u.setActive(!u.isActive());
        utilisateurRepository.save(u);
    }
}
