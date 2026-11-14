package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class ChatRegisterConcurrencyTest {
	@Autowired ChatRegister register;
	@Autowired ChatFinder finder;
	@Autowired UserRepository users;
	@Autowired JdbcTemplate jdbc;
	@Autowired PlatformTransactionManager transactions;
	private Long userId;

	@BeforeEach
	void 회원을_등록한다() {
		userId = users.save(User.registerUser()).getId();
	}

	@AfterEach
	void 정리한다() {
		jdbc.update("DELETE FROM support_messages WHERE conversation_id = ?", userId);
		jdbc.update("DELETE FROM support_conversations WHERE id = ?", userId);
		users.deleteById(userId);
	}

	@Test
	void 동시에_대화를_열어도_하나의_대화를_반환한다() throws Exception {
		try (var executor = Executors.newSingleThreadExecutor()) {
			var started = new CountDownLatch(1);
			var next = new AtomicReference<Future<ChatConversation>>();
			var first = new TransactionTemplate(transactions).execute(status -> {
				var conversation = register.open(userId);
				next.set(executor.submit(() -> {
					started.countDown();
					return register.open(userId);
				}));
				assertBlocked(started, next.get());
				return conversation;
			});
			assertThat(next.get().get(5, TimeUnit.SECONDS)).isEqualTo(first);
			assertThat(jdbc.queryForObject("SELECT count(*) FROM support_conversations WHERE id = ?",
					Long.class, userId)).isEqualTo(1);
		}
	}

	@Test
	void 같은_UUID를_동시에_전송해도_하나의_메시지만_저장한다() throws Exception {
		register.open(userId);
		var request = new ChatMessageRequest(UUID.randomUUID(), "문의");
		try (var executor = Executors.newSingleThreadExecutor()) {
			var started = new CountDownLatch(1);
			var next = new AtomicReference<Future<ChatMessage>>();
			var first = new TransactionTemplate(transactions).execute(status -> {
				var message = register.send(userId, userId, request);
				next.set(executor.submit(() -> {
					started.countDown();
					return register.send(userId, userId, request);
				}));
				assertBlocked(started, next.get());
				return message;
			});
			assertThat(next.get().get(5, TimeUnit.SECONDS)).isEqualTo(first);
			assertThat(finder.newMessages(userId, userId, 0)).containsExactly(first);
		}
	}

	private void assertBlocked(CountDownLatch started, Future<?> request) {
		try {
			assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
		assertThatThrownBy(() -> request.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
	}
}
