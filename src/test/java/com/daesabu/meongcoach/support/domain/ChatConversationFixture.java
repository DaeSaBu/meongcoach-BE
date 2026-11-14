package com.daesabu.meongcoach.support.domain;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public class ChatConversationFixture {
	public static ChatConversation create(long id, Instant updatedAt) {
		var entity = new ChatConversation();
		ReflectionTestUtils.setField(entity, "id", id);
		ReflectionTestUtils.setField(entity, "updatedAt", updatedAt);
		return entity;
	}
}
