package com.annotation.repository;

import com.annotation.model.Dataset;
import com.annotation.model.Tache;
import com.annotation.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {
    List<Tache> findByAnnotateur(Utilisateur annotateur);
    List<Tache> findByAnnotateurAndTerminee(Utilisateur annotateur, boolean terminee);
    List<Tache> findByDataset(Dataset dataset);

    @Query("SELECT COUNT(t) FROM Tache t WHERE t.annotateur = :annotateur AND t.terminee = true")
    long countTermineesParAnnotateur(@Param("annotateur") Utilisateur annotateur);

    boolean existsByExempleIdAndAnnotateurId(Long exempleId, Long annotateurId);

    long countByDatasetAndTerminee(Dataset dataset, boolean terminee);
}
