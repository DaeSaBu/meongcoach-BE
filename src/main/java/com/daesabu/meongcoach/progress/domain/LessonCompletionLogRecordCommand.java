package com.daesabu.meongcoach.progress.domain;

public record LessonCompletionLogRecordCommand(Long userId, Long lessonId, Long dogId) {
}
