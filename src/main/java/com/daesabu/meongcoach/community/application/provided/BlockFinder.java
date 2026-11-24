package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.BlockedUserResult;
import java.util.List;

/**
 * 사용자 차단 조회 능력.
 */
public interface BlockFinder {

	/**
	 * 회원이 차단한 회원을 최근에 차단한 순서로 작성자 정보와 함께 반환한다. 작성자 정보가 없는 회원은 author가 null이다.
	 */
	List<BlockedUserResult> findBlockedUsers(Long userId);

	/**
	 * 회원이 대상 회원을 차단했는지 확인한다.
	 */
	boolean isBlocked(Long userId, Long targetUserId);
}
