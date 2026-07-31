package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 커리큘럼 세부 조회 결과. 커리큘럼에 속한 레슨을 노출 순서대로 담는다.
 */
public record CurriculumDetailView(Long id, Long topicId, String title, int sortOrder, List<LessonView> lessons) {
}
