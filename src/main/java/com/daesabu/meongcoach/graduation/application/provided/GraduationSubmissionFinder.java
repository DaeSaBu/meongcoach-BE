package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.AdminSubmissionResult;
import java.util.List;

/**
 * 운영자의 실습 인증 제출 조회 공개 API.
 */
public interface GraduationSubmissionFinder {

	/**
	 * 주어진 결과(PENDING·APPROVED·REVISION_REQUESTED·REPLACED)의 제출을 오래된 순으로 조회한다.
	 * 결과 값이 올바르지 않으면 InvalidSubmissionResultException을 던진다.
	 */
	List<AdminSubmissionResult> findSubmissions(String result);
}
