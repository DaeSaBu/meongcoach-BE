package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 커리큘럼 리스트 조회 결과. 대상 토픽과 그 토픽의 커리큘럼을 정렬 순서대로 담는다.
 */
public record CurriculumListView(Long topicId, String topicTitle, List<CurriculumView> curriculums) {
}
