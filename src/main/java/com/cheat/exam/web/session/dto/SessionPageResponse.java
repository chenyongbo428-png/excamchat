package com.cheat.exam.web.session.dto;

import java.util.List;

public record SessionPageResponse(
    List<SessionSummaryResponse> items,
    int page,
    int pageSize,
    long total
) {
}
