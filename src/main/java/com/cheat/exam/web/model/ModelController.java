package com.cheat.exam.web.model;

import com.cheat.exam.common.api.ApiResponse;
import com.cheat.exam.service.ModelService;
import com.cheat.exam.web.model.dto.ModelItemResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/enabled")
    public ApiResponse<List<ModelItemResponse>> enabled() {
        return ApiResponse.ok(modelService.listEnabled());
    }
}
