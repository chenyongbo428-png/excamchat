package com.cheat.exam.web.canvas;

import com.cheat.exam.common.api.ApiResponse;
import com.cheat.exam.security.SecurityUtils;
import com.cheat.exam.service.CanvasService;
import com.cheat.exam.web.canvas.dto.AppendCanvasOperationsRequest;
import com.cheat.exam.web.canvas.dto.AppendCanvasOperationsResponse;
import com.cheat.exam.web.canvas.dto.CanvasDocumentResponse;
import com.cheat.exam.web.canvas.dto.SaveCanvasRequest;
import com.cheat.exam.web.canvas.dto.SaveCanvasResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/canvas")
public class CanvasController {

    private final CanvasService canvasService;

    public CanvasController(CanvasService canvasService) {
        this.canvasService = canvasService;
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<CanvasDocumentResponse> get(@PathVariable Long sessionId) {
        return ApiResponse.ok(canvasService.getDocument(SecurityUtils.currentUser(), sessionId));
    }

    @PutMapping("/{sessionId}")
    public ApiResponse<SaveCanvasResponse> save(
        @PathVariable Long sessionId,
        @Valid @RequestBody SaveCanvasRequest request
    ) {
        return ApiResponse.ok(canvasService.saveDocument(SecurityUtils.currentUser(), sessionId, request));
    }

    @PostMapping("/{sessionId}/operations")
    public ApiResponse<AppendCanvasOperationsResponse> appendOperations(
        @PathVariable Long sessionId,
        @Valid @RequestBody AppendCanvasOperationsRequest request
    ) {
        return ApiResponse.ok(canvasService.appendOperations(SecurityUtils.currentUser(), sessionId, request));
    }
}
