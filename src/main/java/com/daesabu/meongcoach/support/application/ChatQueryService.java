package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryService implements ChatFinder {
	private final ChatRepository repository;

	public boolean isOperator() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream()
						.anyMatch(authority -> AuthorityRole.ADMIN.authority().equals(authority.getAuthority()));
	}

	public void verifyExists(long conversationId) {
		if (!repository.exists(conversationId)) {
			throw new ChatAccessDeniedException();
		}
	}

	public List<ChatConversation> conversations(long before, Instant beforeUpdatedAt) {
		if ((before == 0) != (beforeUpdatedAt == null)) {
			throw new ChatCursorInvalidException();
		}
		return repository.conversations(before, beforeUpdatedAt, 50);
	}

	public List<ChatMessage> latestMessages(long conversationId) {
		verifyExists(conversationId);
		return repository.latestMessages(conversationId, 50);
	}

	public List<ChatMessage> newMessages(long conversationId, long after) {
		if (after < 0) {
			throw new ChatCursorInvalidException();
		}
		verifyExists(conversationId);
		return repository.newMessages(conversationId, after, 50);
	}

	public List<ChatMessage> previousMessages(long conversationId, long before) {
		if (before <= 0) {
			throw new ChatCursorInvalidException();
		}
		verifyExists(conversationId);
		return repository.previousMessages(conversationId, before, 50);
	}
}
