package com.cheat.exam.web.canvas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AppendCanvasOperationsRequest(
    @NotEmpty(message = "operations must not be empty")
    List<@Valid CanvasOperationRequest> operations
) {
}
