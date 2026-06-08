package com.annotation.repository;

import com.annotation.model.Annotation;
import com.annotation.model.Dataset;
import com.annotation.model.Exemple;
import com.annotation.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    Optional<Annotation> findByExempleAndAnnotateur(Exemple exemple, Utilisateur annotateur);
    List<Annotation> findByAnnotateur(Utilisateur annotateur);
    List<Annotation> findByExemple(Exemple exemple);

    @Query("SELECT a FROM Annotation a WHERE a.exemple.dataset = :dataset")
    List<Annotation> findByDataset(@Param("dataset") Dataset dataset);

    @Query("SELECT COUNT(a) FROM Annotation a WHERE a.exemple.dataset = :dataset")
    long countByDataset(@Param("dataset") Dataset dataset);
}
