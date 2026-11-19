package com.daesabu.meongcoach.support.application.provided;

public interface ChatRemover {
	/**
	 * 대화를 닫고 대화와 메시지를 보관한다. 이미 닫힌 대화는 처음 종료 시각을 유지한다.
	 * 대화 소유 회원과 상담원이 아니면 접근 거부 예외를 던진다.
	 */
	void close(Long userId, long conversationId);
}
