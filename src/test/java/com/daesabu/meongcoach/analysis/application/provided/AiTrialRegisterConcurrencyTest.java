package com.daesabu.meongcoach.analysis.application.provided;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class AiTrialRegisterConcurrencyTest {

	private static final Long USER_ID = 737001L;

	@Autowired
	private AiTrialRegister aiTrialRegister;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final CompletableFuture<Void> lockReleased = new CompletableFuture<>();

	@AfterEach
	void tearDown() {
		lockReleased.complete(null);
		executor.close();
		aiTrialRepository.findByUserId(USER_ID).ifPresent(aiTrialRepository::delete);
	}

	@Test
	void 같은_사용자의_락이_잡혀_있으면_락이_풀릴_때까지_기다린_뒤_부여_횟수를_늘린다() throws Exception {
		holdLock(USER_ID);

		Future<?> grant = executor.submit(() -> aiTrialRegister.grant(USER_ID, 10));

		assertThatThrownBy(() -> grant.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		grant.get(5, SECONDS);
		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().maxCount())
				.isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 10);
	}

	private void holdLock(Long userId) throws Exception {
		CompletableFuture<Void> lockAcquired = new CompletableFuture<>();
		executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
			aiTrialRepository.lockByUserId(userId);
			lockAcquired.complete(null);
			lockReleased.join();
		}));
		lockAcquired.get(5, SECONDS);
	}
}
