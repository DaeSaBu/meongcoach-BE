package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 커리큘럼 리스트 조회 결과. 대상 토픽과 그 토픽의 커리큘럼을 정렬 순서대로 담는다.
 * 요청 사용자의 프로필 이미지 URL을 함께 담으며, 없으면 빈 문자열이다.
 */
public record CurriculumListView(Long topicId, String topicTitle, String profileImageUrl,
		List<CurriculumView> curriculums) {
}
