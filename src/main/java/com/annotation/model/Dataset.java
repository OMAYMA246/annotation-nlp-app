package com.annotation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "datasets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomDataset;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    /**
     * Nombre de textes par exemple :
     * 1 → tâche mono-texte (Sentiment Analysis, ...)
     * 2 → tâche bi-texte (NLI, Similarité, ...)
     */
    @Column(nullable = false)
    @Builder.Default
    private int nombreTextesParExemple = 1;

    /**
     * Labels/étiquettes possibles pour cette tâche (séparés par ";")
     */
    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassePossible> classesPossibles = new ArrayList<>();

    /**
     * Exemples du dataset
     */
    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Exemple> exemples = new ArrayList<>();

    /**
     * Annotateurs affectés à ce dataset
     */
    @ManyToMany
    @JoinTable(
        name = "dataset_annotateurs",
        joinColumns = @JoinColumn(name = "dataset_id"),
        inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    @Builder.Default
    private Set<Utilisateur> annotateurs = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
