package com.cheat.exam.web.replay;

import com.cheat.exam.common.api.ApiResponse;
import com.cheat.exam.security.SecurityUtils;
import com.cheat.exam.service.ReplayService;
import com.cheat.exam.web.replay.dto.ReplayResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<ReplayResponse> get(@PathVariable Long sessionId) {
        return ApiResponse.ok(replayService.getReplay(SecurityUtils.currentUser(), sessionId));
    }
}
