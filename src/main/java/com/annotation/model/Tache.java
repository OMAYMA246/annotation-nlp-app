package com.annotation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "taches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exemple_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Exemple exemple;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annotateur_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Utilisateur annotateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Dataset dataset;

    @Column(nullable = false)
    @Builder.Default
    private boolean terminee = false;
}
