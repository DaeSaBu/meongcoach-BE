package com.daesabu.meongcoach.community.application.provided;

/**
 * 사용자 차단·해제 능력.
 */
public interface BlockRegister {

	/**
	 * 회원이 대상 회원을 차단한 기록을 저장한다. 이미 차단한 회원이면 아무것도 하지 않는다. 대상이 자기 자신이면 {@code SelfBlockNotAllowedException}, 대상 회원이 없으면
	 * {@code UserNotFoundException}을 던진다.
	 */
	void block(Long userId, Long targetUserId);

	/**
	 * 회원이 대상 회원을 차단한 기록을 삭제한다. 차단하지 않은 회원이면 아무것도 하지 않는다.
	 */
	void unblock(Long userId, Long targetUserId);
}
