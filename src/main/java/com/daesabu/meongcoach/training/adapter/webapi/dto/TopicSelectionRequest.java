package com.daesabu.meongcoach.training.adapter.webapi.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 커리큘럼 화면 변경 요청. 표시할 토픽 ID를 담는다.
 */
public record TopicSelectionRequest(@NotNull Long topicId) {
}
