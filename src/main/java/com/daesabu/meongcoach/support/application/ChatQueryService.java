package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatAccess;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryService implements ChatFinder {
	private final ChatRepository repository;
	private final RegisteredUserChecker users;

	public boolean isSocketAuthorized(ChatAccess access) {
		var role = users.findRole(access.userId());
		return role.isPresent() && (role.get() == AuthorityRole.ADMIN) == access.operator();
	}

	public boolean isOperator(Long userId) {
		return users.findRole(userId).filter(role -> role == AuthorityRole.ADMIN).isPresent();
	}

	public void verifyAccess(Long userId, long conversationId) {
		if (users.findRole(userId).isEmpty()) {
			throw new ChatAccessDeniedException();
		}
		if (userId != conversationId && !isOperator(userId)) {
			throw new ChatAccessDeniedException();
		}
		if (!repository.exists(conversationId)) {
			throw new ChatAccessDeniedException();
		}
	}

	public List<ChatConversation> conversations(Long userId, long before) {
		if (!isOperator(userId)) {
			throw new ChatAccessDeniedException();
		}
		return repository.conversations(before, 50);
	}

	public List<ChatMessage> messages(Long userId, long conversationId, long after, long before) {
		if (after < -1 || before < 0 || (after >= 0 && before > 0)) {
			throw new com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException();
		}
		verifyAccess(userId, conversationId);
		return repository.messages(conversationId, after, before, 50);
	}
}
