package com.annotation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "classes_possibles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassePossible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelleClasse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Dataset dataset;
}
