package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRemover;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRemovalModifyService implements ChatRemover {
	private final ChatFinder finder;

	@Override
	@Transactional
	public void leave(Long userId, long conversationId) {
		var conversation = finder.findLockedConversation(userId, conversationId);
		conversation.verifyAccess(userId, false);
		conversation.leave();
	}
}
