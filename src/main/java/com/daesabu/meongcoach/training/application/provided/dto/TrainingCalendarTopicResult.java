package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;

public record TrainingCalendarTopicResult(Topic topic, List<TrainingCalendarCurriculumResult> curriculums) {
}
