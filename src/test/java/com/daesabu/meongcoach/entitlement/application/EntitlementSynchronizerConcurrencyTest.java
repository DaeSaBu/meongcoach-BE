package com.daesabu.meongcoach.entitlement.application;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.util.List;
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
class EntitlementSynchronizerConcurrencyTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;

	@Autowired
	private EntitlementSynchronizer entitlementSynchronizer;

	@Autowired
	private ActiveEntitlementReader activeEntitlementReader;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final CompletableFuture<Void> lockReleased = new CompletableFuture<>();

	@AfterEach
	void tearDown() {
		lockReleased.complete(null);
		executor.close();
		entitlementRepository.deleteAll();
	}

	@Test
	void 같은_회원의_락이_잡혀_있으면_락이_풀릴_때까지_기다린_뒤_동기화한다() throws Exception {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(new ActiveEntitlement(EntitlementType.PUPPY, null)));
		holdLock(USER_ID);

		Future<?> synchronization = executor.submit(() -> entitlementSynchronizer.synchronize(USER_ID));

		assertThatThrownBy(() -> synchronization.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		synchronization.get(5, SECONDS);
		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getType)
				.isEqualTo(EntitlementType.PUPPY);
	}

	@Test
	void 다른_회원의_락은_동기화를_막지_않는다() throws Exception {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(new ActiveEntitlement(EntitlementType.PUPPY, null)));
		holdLock(OTHER_USER_ID);

		Future<?> synchronization = executor.submit(() -> entitlementSynchronizer.synchronize(USER_ID));

		synchronization.get(5, SECONDS);
		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getType)
				.isEqualTo(EntitlementType.PUPPY);
	}

	private void holdLock(Long userId) throws Exception {
		CompletableFuture<Void> lockAcquired = new CompletableFuture<>();
		executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
			entitlementRepository.lockByUserId(userId);
			lockAcquired.complete(null);
			lockReleased.join();
		}));
		lockAcquired.get(5, SECONDS);
	}
}
