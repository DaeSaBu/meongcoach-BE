package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;
import java.util.Set;

public record CurriculumListResult(Topic topic, List<Curriculum> curriculums, Set<Long> completedLessonIds) {
}
