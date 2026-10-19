package com.daesabu.meongcoach.user.application.provided;

import java.util.Collection;
import java.util.List;

/**
 * 사용자 프로필 요약 조회 능력.
 */
public interface UserProfileFinder {

	/**
	 * 사용자 ID 목록에 해당하는 프로필 요약을 반환한다. 프로필이 없는 사용자는 결과에서 빠진다.
	 */
	List<UserProfileSummaryResult> findSummaries(Collection<Long> userIds);
}
