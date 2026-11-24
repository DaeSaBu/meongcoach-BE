package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.community.application.required.UserBlockRepository;
import com.daesabu.meongcoach.community.domain.UserBlock;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class BlockRegisterAlertTest {

	private static final Long BLOCKER_ID = 758003L;

	private static final long ASYNC_TIMEOUT_MILLIS = 2000;

	private static final long ASYNC_SETTLE_MILLIS = 300;

	@Autowired
	private BlockRegister blockRegister;

	@Autowired
	private UserBlockRepository userBlockRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AlertSender alertSender;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private Long targetUserId;

	@BeforeEach
	void setUp() {
		User target = userRepository.save(User.registerUser());
		targetUserId = target.getId();
	}

	@AfterEach
	void tearDown() {
		userBlockRepository.deleteAll();
		userRepository.deleteById(targetUserId);
	}

	@Test
	void 차단이_커밋되면_차단한_회원과_차단된_회원을_알린다() {
		blockRegister.block(BLOCKER_ID, targetUserId);

		UserBlock block = userBlockRepository.findAll().getFirst();
		ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
		verify(alertSender, timeout(ASYNC_TIMEOUT_MILLIS)).send(captor.capture());
		Alert alert = captor.getValue();
		assertThat(alert.title()).isEqualTo("사용자 차단");
		assertThat(alert.fields()).containsExactly(
				AlertField.of("blockId", block.getId()),
				AlertField.of("blockerId", BLOCKER_ID),
				AlertField.of("blockedId", targetUserId));
	}

	@Test
	void 이미_차단한_회원을_다시_차단하면_한_번만_알린다() {
		blockRegister.block(BLOCKER_ID, targetUserId);
		verify(alertSender, timeout(ASYNC_TIMEOUT_MILLIS)).send(any());

		blockRegister.block(BLOCKER_ID, targetUserId);

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).times(1)).send(any());
	}

	@Test
	void 차단_트랜잭션이_롤백되면_알리지_않는다() {
		transactionTemplate.executeWithoutResult(status -> {
			blockRegister.block(BLOCKER_ID, targetUserId);
			status.setRollbackOnly();
		});

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}
}
