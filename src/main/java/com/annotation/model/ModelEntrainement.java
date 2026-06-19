package com.annotation.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "modeles_entrainement")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ModelEntrainement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Dataset dataset;
    @Column(nullable = false)
    private LocalDateTime dateEntrainement;
    @Column(nullable = false)
    private String statut;
    private Double accuracy;
    private Double f1Score;
    private Integer trainSize;
    private Integer testSize;
    @Column(columnDefinition = "TEXT") private String classesJson;
    @Column(columnDefinition = "TEXT") private String confusionMatrixJson;
    @Column(columnDefinition = "TEXT") private String rapportJson;
    @Column(columnDefinition = "TEXT") private String logsEntrainement;
    @Column(columnDefinition = "TEXT") private String messageErreur;
    @PrePersist protected void onCreate() { dateEntrainement = LocalDateTime.now(); }
}
