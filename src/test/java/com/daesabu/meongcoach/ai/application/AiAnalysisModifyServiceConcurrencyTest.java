package com.daesabu.meongcoach.ai.application;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.application.required.AiTrialRepository;
import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class AiAnalysisModifyServiceConcurrencyTest {

	private static final Long USER_ID = 687001L;
	private static final VideoAnalysisResult RESULT = new VideoAnalysisResult("제목", "본문");

	@Autowired
	private AiAnalysisModifyService aiAnalysisModifyService;

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

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
		aiAnalysisRepository.deleteAll(aiAnalysisRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(USER_ID));
		aiTrialRepository.findByUserId(USER_ID).ifPresent(aiTrialRepository::delete);
	}

	@Test
	void 같은_사용자의_락이_잡혀_있으면_락이_풀릴_때까지_기다린_뒤_사용_횟수를_올린다() throws Exception {
		AiAnalysis analysis = persistStartedAnalysis("videos/training/687001/first.mp4");
		holdLock(USER_ID);

		Future<?> completion = executor.submit(() -> aiAnalysisModifyService.complete(analysis, RESULT));

		assertThatThrownBy(() -> completion.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		completion.get(5, SECONDS);
		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().getUsedCount()).isEqualTo(1);
	}

	@Test
	void 현황이_없는_사용자의_분석_두_건이_동시에_완료돼도_사용_횟수는_2가_된다() throws Exception {
		AiAnalysis first = persistStartedAnalysis("videos/training/687001/first.mp4");
		AiAnalysis second = persistStartedAnalysis("videos/training/687001/second.mp4");
		CountDownLatch ready = new CountDownLatch(1);

		Future<?> firstCompletion = executor.submit(() -> completeWhenReady(ready, first));
		Future<?> secondCompletion = executor.submit(() -> completeWhenReady(ready, second));
		ready.countDown();

		firstCompletion.get(5, SECONDS);
		secondCompletion.get(5, SECONDS);
		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().getUsedCount()).isEqualTo(2);
	}

	private void completeWhenReady(CountDownLatch ready, AiAnalysis analysis) {
		try {
			ready.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
		aiAnalysisModifyService.complete(analysis, RESULT);
	}

	private AiAnalysis persistStartedAnalysis(String videoObjectKey) {
		AiAnalysis analysis = AiAnalysis.uploading(
				new AiAnalysisUploadCommand(USER_ID, videoObjectKey, Instant.now().plusSeconds(900), null));
		analysis.startAnalysis();
		return aiAnalysisRepository.saveAndFlush(analysis);
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
