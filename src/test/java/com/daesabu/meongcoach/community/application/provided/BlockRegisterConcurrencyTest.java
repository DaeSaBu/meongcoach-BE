package com.daesabu.meongcoach.community.application.provided;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.required.UserBlockRepository;
import com.daesabu.meongcoach.community.domain.UserBlock;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class BlockRegisterConcurrencyTest {

	private static final Long BLOCKER_ID = 758002L;

	@Autowired
	private BlockRegister blockRegister;

	@Autowired
	private UserBlockRepository userBlockRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final CompletableFuture<Void> lockReleased = new CompletableFuture<>();
	private Long targetUserId;

	@BeforeEach
	void setUp() {
		User target = userRepository.save(User.registerUser());
		targetUserId = target.getId();
	}

	@AfterEach
	void tearDown() {
		lockReleased.complete(null);
		executor.close();
		userBlockRepository.deleteAll();
		userRepository.deleteById(targetUserId);
	}

	@Test
	void 같은_회원을_차단하는_중이면_끝날_때까지_기다린_뒤_기록을_하나만_남긴다() throws Exception {
		blockHoldingLock();

		Future<?> block = executor.submit(() -> blockRegister.block(BLOCKER_ID, targetUserId));

		assertThatThrownBy(() -> block.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		block.get(5, SECONDS);
		assertThat(userBlockRepository.findAll()).hasSize(1);
	}

	private void blockHoldingLock() throws Exception {
		CompletableFuture<Void> lockAcquired = new CompletableFuture<>();
		executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
			userBlockRepository.lockByBlockerId(BLOCKER_ID);
			userBlockRepository.saveAndFlush(UserBlock.register(BLOCKER_ID, targetUserId));
			lockAcquired.complete(null);
			lockReleased.join();
		}));
		lockAcquired.get(5, SECONDS);
	}
}
