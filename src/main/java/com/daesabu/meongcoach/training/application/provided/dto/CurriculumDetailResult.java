package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.Map;

public record CurriculumDetailResult(Curriculum curriculum, Map<Long, Integer> completedCounts) {
}
