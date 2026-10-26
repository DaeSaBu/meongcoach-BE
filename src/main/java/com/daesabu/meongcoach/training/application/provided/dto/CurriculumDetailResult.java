package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgresses;

public record CurriculumDetailResult(Curriculum curriculum, LessonProgresses progresses) {
}
