package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.List;

public interface PlanCurriculumFinder {

	/**
	 * 요청한 ID 순서로 커리큘럼 설명과 잠금 여부를 반환한다.
	 * 각 커리큘럼의 레슨은 현재 교육 순서로 반환한다.
	 * 존재하지 않는 커리큘럼은 CurriculumNotFoundException을 던진다.
	 * 레슨이 없는 커리큘럼은 IllegalStateException을 던진다.
	 */
	List<PlanCurriculumResult> find(Long userId, List<Long> curriculumIds);

	/**
	 * 요청한 커리큘럼 순서와 각 커리큘럼의 현재 교육 순서로 레슨 ID를 반환한다.
	 * 이용권과 학습 기록은 조회하지 않는다. 빈 목록이면 빈 결과를 반환한다.
	 * 존재하지 않는 커리큘럼은 {@code CurriculumNotFoundException}을 던진다.
	 * 레슨이 없는 커리큘럼은 {@code IllegalStateException}을 던진다.
	 */
	List<Long> findLessonIds(List<Long> curriculumIds);

	/**
	 * 요청한 커리큘럼 순서대로 각 커리큘럼이 속한 토픽을 첫 등장 순서로 중복 없이 반환한다.
	 * 빈 목록이면 빈 결과를 반환한다.
	 * 존재하지 않는 커리큘럼은 {@code CurriculumNotFoundException}을 던진다.
	 */
	List<TopicResult> findTopics(List<Long> curriculumIds);
}
