package com.annotation.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Exemple représente un exemple du dataset à annoter.
 * Il contient une liste de textes :
 *  - 1 texte  → tâches comme Sentiment Analysis
 *  - 2 textes → tâches comme NLI (Premise, Hypothesis) ou Similarité (Text1, Text2)
 *  - N textes → toute autre tâche multi-texte
 */
@Entity
@Table(name = "exemples")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exemple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Liste des textes de cet exemple.
     * Stockée dans une table de jointure exemple_textes.
     * Pour Sentiment Analysis : [text]
     * Pour NLI               : [premise, hypothesis]
     * Pour Similarité        : [text1, text2]
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "exemple_textes",
        joinColumns = @JoinColumn(name = "exemple_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "texte", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> textes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Dataset dataset;

    // Helpers
    public String getTexte1() {
        return textes.size() > 0 ? textes.get(0) : "";
    }

    public String getTexte2() {
        return textes.size() > 1 ? textes.get(1) : "";
    }

    public boolean isMultiTexte() {
        return textes.size() > 1;
    }
}
