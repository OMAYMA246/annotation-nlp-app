package com.annotation.repository;
import com.annotation.model.Dataset;
import com.annotation.model.ModelEntrainement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModelEntrainementRepository extends JpaRepository<ModelEntrainement, Long> {
    List<ModelEntrainement> findByDatasetOrderByDateEntrainementDesc(Dataset dataset);
}
