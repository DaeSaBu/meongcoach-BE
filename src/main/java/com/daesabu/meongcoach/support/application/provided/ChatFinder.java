package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatFinder {
	ChatConversation findMemberConversation(long memberId);

	ChatConversation findConversation(Long userId, long conversationId);

	ChatConversation findLockedConversation(Long userId, long conversationId);

	List<ChatConversation> conversations(long before, LocalDateTime beforeUpdatedAt);

	/**
	 * 종료된 대화를 최근 활동 시각과 ID 내림차순으로 최대 50건 반환한다.
	 * 최초 조회는 before 0과 beforeUpdatedAt null을 받으며, 둘 중 하나만 전달하면 커서 예외를 던진다.
	 */
	List<ChatConversation> closedConversations(long before, LocalDateTime beforeUpdatedAt);

	/**
	 * 회원 본인의 진행 중·종료된 대화를 최근 활동 시각과 ID 내림차순으로 최대 50건 반환한다.
	 * 최초 조회는 before 0과 beforeUpdatedAt null을 받으며, 둘 중 하나만 전달하면 커서 예외를 던진다.
	 */
	List<ChatConversation> memberConversations(Long memberId, long before, LocalDateTime beforeUpdatedAt);

	List<ChatMessage> latestMessages(Long userId, long conversationId);

	List<ChatMessage> newMessages(Long userId, long conversationId, long after);

	List<ChatMessage> previousMessages(Long userId, long conversationId, long before);

	boolean isOperator();
}
