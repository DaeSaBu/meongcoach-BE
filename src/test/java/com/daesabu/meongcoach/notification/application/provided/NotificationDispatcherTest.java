package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.BatchResult;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@NonTransactionalApplicationTest
class NotificationDispatcherTest {

	@Autowired NotificationDispatcher dispatcher;
	@Autowired NotificationSender sender;
	@Autowired UserRepository users;
	@Autowired DeviceInstallationRepository fids;
	@Autowired NotificationPreferenceRepository preferences;
	@Autowired NotificationDispatchRepository dispatches;

	private Long userId;
	private String fid;
	private NotificationDispatchRequest request;

	@BeforeEach
	void 준비한다() {
		userId = users.save(User.registerUser()).getId();
		fid = UUID.randomUUID().toString();
		request = new NotificationDispatchRequest(Instant.now(), 17);
		fids.save(DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand(fid, DevicePlatform.IOS), Instant.now()));
		preferences.save(NotificationPreferenceFixture.create(userId, 17, true));
	}

	@AfterEach
	void 정리한다() {
		fids.deleteAll(fids.findAllByUserIdInOrderByUserIdAscIdAsc(List.of(userId)));
		preferences.deleteAll(preferences.findAllByUserId(userId));
		users.deleteById(userId);
		dispatches.findByDispatchKey(request.key()).ifPresent(dispatches::delete);
	}

	@Test
	void 발송_후_무효_FID을_삭제한다() {
		BatchResult deliveryResults = results(List.of(fid));
		given(sender.send(anyMap())).willReturn(List.of(deliveryResults));

		NotificationDispatchResult result = dispatcher.dispatch(request);

		assertThat(result.fids()).isEqualTo(1);
		assertThat(result.duplicate()).isFalse();
		var history = dispatches.findByDispatchKey(request.key()).orElseThrow();
		assertThat(history.getId()).isPositive();
		assertThat(history.getDispatchKey()).isEqualTo(request.key());
		assertThat(history.getCreatedAt()).isNotNull();
		assertThat(history.getUpdatedAt()).isNotNull();
		assertThat(fids.findByFid(fid)).isEmpty();
	}

	@Test
	void 같은_예약_시각의_호출은_한_번만_발송한다() {
		BatchResult deliveryResults = results(List.of());
		given(sender.send(anyMap())).willReturn(List.of(deliveryResults));

		dispatcher.dispatch(request);
		NotificationDispatchResult repeated = dispatcher.dispatch(request);

		assertThat(repeated.duplicate()).isTrue();
		verify(sender, times(1)).send(anyMap());
	}

	@Test
	void 실패한_예약은_다시_발송하고_완료한_예약은_재발송하지_않는다() {
		var failure = new NotificationDeliveryException(new IllegalStateException("FCM unavailable"));
		BatchResult deliveryResults = results(List.of());
		given(sender.send(anyMap())).willThrow(failure)
				.willReturn(List.of(deliveryResults));

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isSameAs(failure);
		assertThat(dispatches.existsByDispatchKey(request.key())).isFalse();
		assertThat(dispatcher.dispatch(request).duplicate()).isFalse();
		assertThat(dispatches.existsByDispatchKey(request.key())).isTrue();
		assertThat(dispatcher.dispatch(request).duplicate()).isTrue();
		verify(sender, times(2)).send(anyMap());
	}

	@Test
	void 발송이_실패하면_등록을_유지하고_완료_이력을_남기지_않는다() {
		var cause = new IllegalStateException("FCM unavailable");
		var failure = new NotificationDeliveryException(cause);
		given(sender.send(anyMap())).willThrow(failure);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isSameAs(failure).hasCause(cause);
		assertThat(fids.findByFid(fid)).isPresent();
		assertThat(dispatches.existsByDispatchKey(request.key())).isFalse();
	}

	@Test
	void 대상이_없으면_FCM을_호출하지_않는다() {
		fids.deleteAll(fids.findAllByUserIdInOrderByUserIdAscIdAsc(List.of(userId)));

		assertThat(dispatcher.dispatch(request).fids()).isZero();
		assertThat(dispatcher.dispatch(request).duplicate()).isTrue();
		verifyNoInteractions(sender);
	}

	@Test
	void 잘못된_시각은_발송_키를_선점하지_않는다() {
		request = new NotificationDispatchRequest(request.scheduledTime(), 24);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isInstanceOf(InvalidNotificationHourException.class);
		assertThat(dispatches.existsByDispatchKey(request.key())).isFalse();
		verifyNoInteractions(sender);
	}

	@Test
	void 첫_요청이_FCM을_기다리는_동안에도_중복_요청은_발송하지_않는다() throws Exception {
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		given(sender.send(anyMap())).willAnswer(invocation -> {
			started.countDown();
			assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			BatchResult batchResult = results(List.of());
			return List.of(batchResult);
		});
		try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
			var first = executor.submit(() -> dispatcher.dispatch(request));
			try {
				assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
				assertThat(dispatcher.dispatch(request).duplicate()).isTrue();
			} finally {
				release.countDown();
			}
			assertThat(first.get(10, java.util.concurrent.TimeUnit.SECONDS).duplicate()).isFalse();
		}
		verify(sender, times(1)).send(anyMap());
	}
	private BatchResult results(List<String> invalidFids) {
		BatchResult results = mock(BatchResult.class);
		given(results.invalidFids()).willReturn(invalidFids);
		return results;
	}

}
