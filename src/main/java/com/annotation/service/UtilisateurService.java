package com.annotation.service;

import com.annotation.model.Role;
import com.annotation.model.Utilisateur;
import com.annotation.repository.RoleRepository;
import com.annotation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private static final String CARACTERES_MDP =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LONGUEUR_MDP = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

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

    /**
     * Génère un mot de passe aléatoire lisible (sans caractères ambigus comme 0/O, 1/l/I).
     */
    private String genererMotDePasse() {
        StringBuilder sb = new StringBuilder(LONGUEUR_MDP);
        for (int i = 0; i < LONGUEUR_MDP; i++) {
            sb.append(CARACTERES_MDP.charAt(RANDOM.nextInt(CARACTERES_MDP.length())));
        }
        return sb.toString();
    }

    /**
     * Crée un annotateur avec un mot de passe généré automatiquement.
     * Retourne le mot de passe en clair (uniquement disponible à cet instant,
     * pour affichage à l'administrateur) via Utilisateur.motDePasseGenere (champ transitoire).
     */
    @Transactional
    public ResultatCreationAnnotateur creerAnnotateur(String nom, String prenom, String login) {
        if (utilisateurRepository.existsByLogin(login)) {
            throw new RuntimeException("Login déjà utilisé: " + login);
        }
        Role role = roleRepository.findByNomRole("ANNOTATOR_ROLE")
            .orElseThrow(() -> new RuntimeException("Rôle ANNOTATOR_ROLE introuvable"));

        String motDePasseClair = genererMotDePasse();

        Utilisateur u = Utilisateur.builder()
            .nom(nom)
            .prenom(prenom)
            .login(login)
            .password(passwordEncoder.encode(motDePasseClair))
            .active(true)
            .roles(Set.of(role))
            .build();
        u = utilisateurRepository.save(u);

        return new ResultatCreationAnnotateur(u, motDePasseClair);
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
        Utilisateur u = findById(id);
        u.setActive(false); // suppression logique
        utilisateurRepository.save(u);
    }
    @Transactional
    public void toggleActive(Long id) {
        Utilisateur u = findById(id);
        u.setActive(!u.isActive());
        utilisateurRepository.save(u);
    }

    /**
     * Petit conteneur pour renvoyer à la fois l'utilisateur créé
     * et son mot de passe en clair (à n'afficher qu'une seule fois).
     */
    public record ResultatCreationAnnotateur(Utilisateur utilisateur, String motDePasseClair) {}
}