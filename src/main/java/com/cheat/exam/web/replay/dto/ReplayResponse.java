package com.cheat.exam.web.replay.dto;

import java.util.List;

public record ReplayResponse(
    ReplaySessionResponse session,
    ReplayBackgroundImageResponse backgroundImage,
    List<ReplayTimelineItemResponse> timeline
) {
}
