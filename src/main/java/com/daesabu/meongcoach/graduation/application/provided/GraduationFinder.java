package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import java.util.List;

/**
 * 토픽 수료 조회 공개 API.
 */
public interface GraduationFinder {

	/**
	 * 한 토픽의 실습 인증 현황을 조회한다. 제출한 적이 없어도 토픽과 자격을 담아 돌려준다.
	 * 토픽이 없으면 TopicNotFoundException을 던진다.
	 */
	GraduationDetailResult findGraduation(Long userId, Long topicId);

	/**
	 * 회원이 제출한 토픽과 학습 완료한 미제출 토픽의 현황을 조회한다. 제출은 최신 순, 미제출은 뒤에 교육 순서로 반환한다.
	 * 영상 URL은 발급하지 않는다.
	 */
	List<GraduationSummaryResult> findGraduations(Long userId);
}
