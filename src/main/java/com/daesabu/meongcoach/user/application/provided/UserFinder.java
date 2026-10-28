package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserFinder {
	boolean isActiveUser(Long userId);

	User findById(Long userId);

	/**
	 * 회원 ID 목록에 해당하는 회원의 ID와 프로필 닉네임·이미지를 반환한다. 프로필이 없는 회원은 결과에서 빠진다.
	 */
	List<UserProfileSummaryResult> findProfileSummaries(Collection<Long> userIds);

	/**
	 * 회원 ID 목록 중 상태가 활성인 회원의 ID만 반환한다. 없는 회원과 탈퇴 회원은 결과에서 빠진다.
	 */
	Set<Long> findActiveUserIds(Collection<Long> userIds);
}
