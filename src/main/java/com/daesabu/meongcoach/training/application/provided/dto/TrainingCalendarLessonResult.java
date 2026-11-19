package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Lesson;

public record TrainingCalendarLessonResult(Lesson lesson, int trainedDays) {
}
