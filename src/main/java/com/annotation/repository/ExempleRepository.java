package com.annotation.repository;

import com.annotation.model.Dataset;
import com.annotation.model.Exemple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExempleRepository extends JpaRepository<Exemple, Long> {
    List<Exemple> findByDataset(Dataset dataset);
    long countByDataset(Dataset dataset);
}
