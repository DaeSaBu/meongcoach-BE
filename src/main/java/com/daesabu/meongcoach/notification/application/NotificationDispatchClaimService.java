package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDispatchClaimService {
	private final NotificationDispatchRepository repository;

	@Transactional
	public boolean claim(String key) {
		return repository.claim(key, Instant.now()) == 1;
	}
}
