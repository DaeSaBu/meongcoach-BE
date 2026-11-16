package com.daesabu.meongcoach.training.domain;

import java.time.Instant;

public record TrainedLessonRecordCommand(
		Long dogId,
		Long lessonId,
		TrainingSource source,
		Instant completedAt) {
}
