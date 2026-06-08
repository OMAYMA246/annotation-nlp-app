package com.annotation.config;

import com.annotation.model.Role;
import com.annotation.model.Utilisateur;
import com.annotation.repository.RoleRepository;
import com.annotation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // ── 1. Créer les rôles si absents ──────────────────────────────────────
        Role adminRole = roleRepository.findByNomRole("ADMIN_ROLE")
            .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN_ROLE")));

        Role annotatorRole = roleRepository.findByNomRole("ANNOTATOR_ROLE")
            .orElseGet(() -> roleRepository.save(new Role(null, "ANNOTATOR_ROLE")));

        // ── 2. Compte administrateur : admin / admin ───────────────────────────
        creerUtilisateur("Admin", "Système", "admin", "admin", Set.of(adminRole));

        // ── 3. Comptes annotateurs : user1/user1, user2/user2, user3/user3 ─────
        List<String[]> annotateurs = List.of(
            new String[]{"User",  "One",   "user1", "user1"},
            new String[]{"User",  "Two",   "user2", "user2"},
            new String[]{"User",  "Three", "user3", "user3"}
        );

        for (String[] u : annotateurs) {
            creerUtilisateur(u[0], u[1], u[2], u[3], Set.of(annotatorRole));
        }

        log.info("=================================================");
        log.info("  Comptes initialisés :");
        log.info("  admin / admin  (Administrateur)");
        log.info("  user1 / user1  (Annotateur)");
        log.info("  user2 / user2  (Annotateur)");
        log.info("  user3 / user3  (Annotateur)");
        log.info("=================================================");
    }

    private void creerUtilisateur(String nom, String prenom, String login,
                                   String password, Set<Role> roles) {
        if (!utilisateurRepository.existsByLogin(login)) {
            Utilisateur u = Utilisateur.builder()
                .nom(nom)
                .prenom(prenom)
                .login(login)
                .password(passwordEncoder.encode(password))
                .active(true)
                .roles(roles)
                .build();
            utilisateurRepository.save(u);
            log.info("✅ Compte créé : {}", login);
        }
    }
}
