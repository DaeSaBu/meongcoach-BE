package com.daesabu.meongcoach.graduation.application.provided.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;

/**
 * 한 토픽의 실습 인증 현황. 제출한 적이 없으면 graduation·latestSubmission·latestVideoUrl이 null이다.
 */
public record GraduationDetailResult(TopicResult topic, boolean eligible, TopicGraduation graduation,
                                     GraduationSubmission latestSubmission, String latestVideoUrl) {

	public static GraduationDetailResult notSubmitted(TopicResult topic, boolean eligible) {
		return new GraduationDetailResult(topic, eligible, null, null, null);
	}
}
