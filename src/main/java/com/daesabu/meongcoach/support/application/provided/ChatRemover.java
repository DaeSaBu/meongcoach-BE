package com.daesabu.meongcoach.support.application.provided;

public interface ChatRemover {
	/**
	 * 본인 소유 대화를 종료하고 대화와 메시지를 보관한다. 다른 회원의 대화에는 접근 거부 예외를 던진다.
	 */
	void leave(Long userId, long conversationId);
}
