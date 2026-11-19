package com.daesabu.meongcoach.analysis.application.provided;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.application.required.PurchasedTrialCountReader;
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
class AiTrialSynchronizerConcurrencyTest {

	private static final Long USER_ID = 742001L;

	private static final Long OTHER_USER_ID = 742002L;

	@Autowired
	private AiTrialSynchronizer aiTrialSynchronizer;

	@Autowired
	private PurchasedTrialCountReader purchasedTrialCountReader;

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
	void 같은_사용자의_락이_잡혀_있으면_락이_풀릴_때까지_기다린_뒤_구매_횟수를_맞춘다() throws Exception {
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(20);
		holdLock(USER_ID);

		Future<?> synchronization = executor.submit(() -> aiTrialSynchronizer.synchronize(USER_ID));

		assertThatThrownBy(() -> synchronization.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		synchronization.get(5, SECONDS);
		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().getPurchasedCount()).isEqualTo(20);
	}

	@Test
	void 다른_사용자의_락은_동기화를_막지_않는다() throws Exception {
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(20);
		holdLock(OTHER_USER_ID);

		Future<?> synchronization = executor.submit(() -> aiTrialSynchronizer.synchronize(USER_ID));

		synchronization.get(5, SECONDS);
		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().getPurchasedCount()).isEqualTo(20);
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
