package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.application.required.ChatMessageRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import java.time.Instant;
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

	public ChatConversation findConversation(long conversationId) {
		return conversations.findById(conversationId).orElseThrow(ChatAccessDeniedException::new);
	}

	@Transactional
	public ChatConversation findLockedConversation(long conversationId) {
		return conversations.findLockedById(conversationId).orElseThrow(ChatAccessDeniedException::new);
	}

	public boolean isOperator() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream()
						.anyMatch(authority -> AuthorityRole.ADMIN.authority().equals(authority.getAuthority())
								|| AuthorityRole.EXPERT.authority().equals(authority.getAuthority()));
	}

	public void verifyExists(long conversationId) {
		if (!conversations.existsById(conversationId)) {
			throw new ChatAccessDeniedException();
		}
	}

	public List<ChatConversation> conversations(long before, Instant beforeUpdatedAt) {
		if ((before == 0) != (beforeUpdatedAt == null)) {
			throw new ChatCursorInvalidException();
		}
		var page = PageRequest.of(0, 50);
		if (before == 0) {
			return conversations.findAllByOrderByUpdatedAtDescIdDesc(page);
		}
		return conversations.findPrevious(beforeUpdatedAt, before, page);
	}

	public List<ChatMessage> latestMessages(long conversationId) {
		verifyExists(conversationId);
		var page = PageRequest.of(0, 50);
		return messages.findByConversationIdOrderByIdDesc(conversationId, page).reversed();
	}

	public List<ChatMessage> newMessages(long conversationId, long after) {
		if (after < 0) {
			throw new ChatCursorInvalidException();
		}
		verifyExists(conversationId);
		var page = PageRequest.of(0, 50);
		return messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, after, page);
	}

	public List<ChatMessage> previousMessages(long conversationId, long before) {
		if (before <= 0) {
			throw new ChatCursorInvalidException();
		}
		verifyExists(conversationId);
		var page = PageRequest.of(0, 50);
		return messages.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, before, page).reversed();
	}
}
