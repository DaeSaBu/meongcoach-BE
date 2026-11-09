package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.application.provided.ChatPushRegister;
import com.daesabu.meongcoach.support.application.required.ChatPushRepository;
import com.daesabu.meongcoach.support.domain.ChatPushJob;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatPushModifyService implements ChatPushRegister {
	private final ChatPushRepository repository;

	@Transactional
	public Optional<ChatPushJob> claim() {
		return repository.claim();
	}

	@Transactional
	public boolean complete(ChatPushJob job) {
		return repository.complete(job);
	}

	@Transactional
	public boolean fail(ChatPushJob job, String error) {
		return repository.fail(job, error);
	}
}
