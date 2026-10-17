package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;

/**
 * 토픽 수료 조회 공개 API.
 */
public interface GraduationFinder {

	/**
	 * 한 토픽의 실습 인증 현황을 조회한다. 제출한 적이 없어도 토픽과 자격을 담아 돌려준다.
	 * 토픽이 없으면 TopicNotFoundException을 던진다.
	 */
	GraduationDetailResult findGraduation(Long userId, Long topicId);
}
