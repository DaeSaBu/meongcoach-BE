package com.daesabu.meongcoach.support.application.required;

import com.daesabu.meongcoach.support.domain.ChatPushJob;

import java.util.Optional;

public interface ChatPushRepository {
	void enqueue(long messageId, long userId);

	Optional<ChatPushJob> claim();

	boolean complete(ChatPushJob job);

	boolean fail(ChatPushJob job, String error);
}
