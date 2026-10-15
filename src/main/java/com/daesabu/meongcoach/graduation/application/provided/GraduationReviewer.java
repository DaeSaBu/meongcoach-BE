package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;

/**
 * 운영자의 실습 인증 판정 공개 API.
 */
public interface GraduationReviewer {

	/**
	 * 검수 대기 중인 제출을 승인하거나 보완 요청하고, 같은 트랜잭션에서 수료 상태도 함께 바꾼다.
	 * 보완 요청은 메모가 필수이고 승인은 선택이다.
	 * 판정 값이 APPROVED·REVISION_REQUESTED가 아니면 InvalidReviewDecisionException, 제출이 없으면 SubmissionNotFoundException,
	 * 검수 대기 중이 아니면 SubmissionNotPendingException, 보완 요청에 메모가 없으면 ReviewNoteRequiredException을 던진다.
	 */
	GraduationSubmission review(Long submissionId, GraduationReviewRequest request);
}
