package com.annotation.repository;

import com.annotation.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByLogin(String login);
    boolean existsByLogin(String login);

    @Query("SELECT u FROM Utilisateur u JOIN u.roles r WHERE r.nomRole = 'ANNOTATOR_ROLE'")
    List<Utilisateur> findAllAnnotateurs();
}
