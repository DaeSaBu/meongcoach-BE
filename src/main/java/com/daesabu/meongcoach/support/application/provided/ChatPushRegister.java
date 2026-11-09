package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatPushJob;

import java.util.Optional;

public interface ChatPushRegister {
	Optional<ChatPushJob> claim();

	boolean complete(ChatPushJob job);

	boolean fail(ChatPushJob job, String error);
}
