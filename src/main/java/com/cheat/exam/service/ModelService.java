package com.cheat.exam.service;

import com.cheat.exam.repository.ModelConfigRepository;
import com.cheat.exam.web.model.dto.ModelItemResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelService {

    private final ModelConfigRepository modelConfigRepository;

    public ModelService(ModelConfigRepository modelConfigRepository) {
        this.modelConfigRepository = modelConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<ModelItemResponse> listEnabled() {
        return modelConfigRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
            .map(model -> new ModelItemResponse(
                model.getModelCode(),
                model.getDisplayName(),
                model.getProviderCode(),
                model.isSupportsVision(),
                model.isSupportsStream()
            ))
            .toList();
    }
}
