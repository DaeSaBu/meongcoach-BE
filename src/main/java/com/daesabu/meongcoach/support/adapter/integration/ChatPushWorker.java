package com.daesabu.meongcoach.support.adapter.integration;

import com.daesabu.meongcoach.support.application.provided.ChatPushDispatcher;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ChatPushWorker {
	private final ChatPushDispatcher dispatcher;
	private volatile boolean running;
	private Thread worker;

	@EventListener(ApplicationReadyEvent.class)
	public void start() {
		running = true;
		worker = Thread.ofPlatform().daemon().name("support-push-worker").start(this::run);
	}

	private void run() {
		while (running) {
			try {
				if (dispatcher.dispatchNext()) {
					continue;
				}
			} catch (RuntimeException exception) {
				log.warn("상담 푸시 작업 처리 실패: {}", exception.getClass().getSimpleName());
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	@PreDestroy
	public void stop() {
		running = false;
		if (worker != null) {
			worker.interrupt();
		}
	}
}
