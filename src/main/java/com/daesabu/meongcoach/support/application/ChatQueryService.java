package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.application.required.ChatMessageRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryService implements ChatFinder {
	private final ChatConversationRepository conversations;
	private final ChatMessageRepository messages;

	public ChatConversation findMemberConversation(long memberId) {
		return conversations.findByMemberId(memberId).orElseThrow(ChatAccessDeniedException::new);
	}

	public ChatConversation findConversation(Long userId, long conversationId) {
		var conversation = conversations.findById(conversationId).orElseThrow(ChatAccessDeniedException::new);
		verifyAccess(conversation, userId);
		return conversation;
	}

	@Transactional
	public ChatConversation findLockedConversation(Long userId, long conversationId) {
		var conversation = conversations.findLockedById(conversationId).orElseThrow(ChatAccessDeniedException::new);
		verifyAccess(conversation, userId);
		return conversation;
	}

	private void verifyAccess(ChatConversation conversation, long userId) {
		if (conversation.isClosed() || (!conversation.isOwnedBy(userId) && !isOperator())) {
			throw new ChatAccessDeniedException();
		}
	}

	public boolean isOperator() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream()
						.anyMatch(authority -> AuthorityRole.ADMIN.authority().equals(authority.getAuthority())
								|| AuthorityRole.EXPERT.authority().equals(authority.getAuthority()));
	}

	public List<ChatConversation> conversations(long before, LocalDateTime beforeUpdatedAt) {
		if ((before == 0) != (beforeUpdatedAt == null)) {
			throw new ChatCursorInvalidException();
		}
		var page = PageRequest.of(0, 50);
		if (before == 0) {
			return conversations.findAllByOrderByLastMessageAtDescIdDesc(page);
		}
		return conversations.findPrevious(beforeUpdatedAt, before, page);
	}

	public List<ChatMessage> latestMessages(Long userId, long conversationId) {
		findConversation(userId, conversationId);
		var page = PageRequest.of(0, 50);
		return messages.findByConversationIdOrderByIdDesc(conversationId, page).reversed();
	}

	public List<ChatMessage> newMessages(Long userId, long conversationId, long after) {
		if (after < 0) {
			throw new ChatCursorInvalidException();
		}
		findConversation(userId, conversationId);
		var page = PageRequest.of(0, 50);
		return messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, after, page);
	}

	public List<ChatMessage> previousMessages(Long userId, long conversationId, long before) {
		if (before <= 0) {
			throw new ChatCursorInvalidException();
		}
		findConversation(userId, conversationId);
		var page = PageRequest.of(0, 50);
		return messages.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, before, page).reversed();
	}
}
