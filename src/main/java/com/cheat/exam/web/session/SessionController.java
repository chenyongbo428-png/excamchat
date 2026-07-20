package com.cheat.exam.web.session;

import com.cheat.exam.common.api.ApiResponse;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.security.SecurityUtils;
import com.cheat.exam.service.SessionService;
import com.cheat.exam.web.session.dto.CreateMessageRequest;
import com.cheat.exam.web.session.dto.CreateSessionRequest;
import com.cheat.exam.web.session.dto.MessageItemResponse;
import com.cheat.exam.web.session.dto.SendMessageResponse;
import com.cheat.exam.web.session.dto.SessionDetailResponse;
import com.cheat.exam.web.session.dto.SessionPageResponse;
import com.cheat.exam.web.session.dto.SessionResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ApiResponse<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(sessionService.create(SecurityUtils.currentUser(), request));
    }

    @GetMapping
    public ApiResponse<SessionPageResponse> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(sessionService.list(SecurityUtils.currentUser(), page, pageSize));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionDetailResponse> detail(@PathVariable Long sessionId) {
        return ApiResponse.ok(sessionService.detail(SecurityUtils.currentUser(), sessionId));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<SendMessageResponse> sendMessage(
        @PathVariable Long sessionId,
        @Valid @RequestBody CreateMessageRequest request
    ) {
        return ApiResponse.ok(sessionService.sendMessage(SecurityUtils.currentUser(), sessionId, request));
    }

    @PostMapping(value = "/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamMessage(
        @PathVariable Long sessionId,
        @Valid @RequestBody CreateMessageRequest request,
        HttpServletResponse response
    ) throws IOException {
        AuthenticatedUser currentUser = SecurityUtils.currentUser();
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        sessionService.streamMessage(currentUser, sessionId, request, response.getOutputStream());
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<MessageItemResponse>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.ok(sessionService.listMessages(SecurityUtils.currentUser(), sessionId));
    }
}
