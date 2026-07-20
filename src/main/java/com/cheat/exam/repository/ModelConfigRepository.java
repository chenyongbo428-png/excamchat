package com.cheat.exam.repository;

import com.cheat.exam.domain.model.ModelConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {

    List<ModelConfig> findByEnabledTrueOrderBySortOrderAsc();

    Optional<ModelConfig> findByModelCodeAndEnabledTrue(String modelCode);
}
