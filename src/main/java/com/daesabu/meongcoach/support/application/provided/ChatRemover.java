package com.daesabu.meongcoach.support.application.provided;

public interface ChatRemover {
	/**
	 * 본인 소유 대화와 메시지를 삭제한다. 다른 회원의 대화에는 접근 거부 예외를 던진다.
	 */
	void leave(Long userId, long conversationId);
}
