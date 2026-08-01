package com.daesabu.meongcoach.training.adapter.webapi.dto;

/**
 * 레슨 진행도 응답. 진행 기록이 없는 레슨은 0으로 내려간다.
 */
public record UserLessonProgressResponse(int completedCount) {
}
