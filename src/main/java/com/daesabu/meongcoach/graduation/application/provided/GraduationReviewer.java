package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

/**
 * 운영자의 실습 인증 판정 공개 API.
 */
public interface GraduationReviewer {

	/**
	 * 검수 대기 중인 토픽 수료를 승인하거나 보완 요청한다. 승인하면 같은 트랜잭션에서 보상 이용권을 결제 대행에 지급하고 만료 시각을 기록한다.
	 * 지급에 실패하면 EntitlementProviderUnavailableException을 던지고 수료는 검수 대기로 남는다.
	 * 보완 요청은 메모가 필수이고 승인은 선택이다.
	 * 판정 값이 APPROVED·REVISION_REQUESTED가 아니면 InvalidReviewDecisionException, 수료가 없으면 GraduationNotFoundException,
	 * 검수 대기 중이 아니면 GraduationNotPendingException, 보완 요청에 메모가 없으면 ReviewNoteRequiredException을 던진다.
	 */
	TopicGraduation review(Long graduationId, GraduationReviewRequest request);
}
