package com.daesabu.meongcoach.support.domain;

import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public class ChatConversationFixture {
	public static ChatConversation create(long id, long memberId, LocalDateTime updatedAt) {
		var entity = new ChatConversation();
		ReflectionTestUtils.setField(entity, "id", id);
		ReflectionTestUtils.setField(entity, "memberId", memberId);
		ReflectionTestUtils.setField(entity, "updatedAt", updatedAt);
		return entity;
	}
}
