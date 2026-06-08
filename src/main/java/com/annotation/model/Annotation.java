package com.annotation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "annotations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exemple_id", "annotateur_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Annotation {

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

    @Column(nullable = false)
    private String classeChoisie;

    @Column(nullable = false)
    private LocalDateTime dateAnnotation;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        dateAnnotation = LocalDateTime.now();
    }
}
