package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationSendResult;
import com.daesabu.meongcoach.notification.application.required.ScheduledNotificationDispatchRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationScheduleFixture;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@NonTransactionalApplicationTest
class ScheduledNotificationDispatcherTest {

	@Autowired ScheduledNotificationDispatcher dispatcher;
	@Autowired NotificationSender sender;
	@Autowired UserRepository users;
	@Autowired DeviceInstallationRepository fids;
	@Autowired NotificationScheduleRepository schedules;
	@Autowired ScheduledNotificationDispatchRepository dispatches;

	@MockitoBean Clock clock;

	private static final Instant NOW = Instant.parse("2026-10-31T08:05:00Z");
	private static final Instant SCHEDULED_AT = Instant.parse("2026-10-31T08:00:00Z");

	private Long userId;
	private String fid;
	private ScheduledNotificationDispatchRequest request;

	@BeforeEach
	void 준비한다() {
		userId = users.save(User.registerUser()).getId();
		fid = UUID.randomUUID().toString();
		given(clock.instant()).willReturn(NOW);
		request = new ScheduledNotificationDispatchRequest(LocalDate.of(2026, 10, 31), 17, 0);
		fids.save(DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand(fid, DevicePlatform.IOS), Instant.now()));
		schedules.save(NotificationScheduleFixture.create(userId, 17, true));
	}

	@AfterEach
	void 정리한다() {
		fids.deleteAll(fids.findAllByUserIdInOrderByUserIdAscIdAsc(List.of(userId)));
		schedules.deleteAll(schedules.findAllByUserId(userId));
		users.deleteById(userId);
		dispatches.findByScheduledAt(SCHEDULED_AT).ifPresent(dispatches::delete);
	}

	@Test
	void 발송_후_무효_FID을_삭제한다() {
		NotificationSendResult deliveryResults = results(List.of(fid));
		given(sender.send(anyList())).willReturn(deliveryResults);

		ScheduledNotificationDispatchResult result = dispatcher.dispatch(request);

		assertThat(result.fids()).isEqualTo(1);
		assertThat(result.duplicate()).isFalse();
		var history = dispatches.findByScheduledAt(SCHEDULED_AT).orElseThrow();
		assertThat(history.getId()).isPositive();
		assertThat(history.scheduled()).isEqualTo(SCHEDULED_AT);
		assertThat(history.getCreatedAt()).isNotNull();
		assertThat(history.getUpdatedAt()).isNotNull();
		assertThat(fids.findByFid(fid)).isEmpty();
	}

	@Test
	void 같은_예약_시각의_호출은_한_번만_발송한다() {
		NotificationSendResult deliveryResults = results(List.of());
		given(sender.send(anyList())).willReturn(deliveryResults);

		dispatcher.dispatch(request);
		ScheduledNotificationDispatchResult repeated = dispatcher.dispatch(request);

		assertThat(repeated.duplicate()).isTrue();
		verify(sender, times(1)).send(anyList());
	}

	@Test
	void 실패한_예약은_다시_발송하고_완료한_예약은_재발송하지_않는다() {
		var cause = new IllegalStateException("FCM unavailable");
		var failure = new NotificationDeliveryException(cause);
		NotificationSendResult deliveryResults = results(List.of());
		given(sender.send(anyList())).willThrow(failure)
				.willReturn(deliveryResults);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isSameAs(failure).hasCause(cause);
		assertThat(dispatches.existsByScheduledAt(SCHEDULED_AT)).isFalse();
		assertThat(dispatcher.dispatch(request).duplicate()).isFalse();
		assertThat(dispatches.existsByScheduledAt(SCHEDULED_AT)).isTrue();
		assertThat(dispatcher.dispatch(request).duplicate()).isTrue();
		verify(sender, times(2)).send(anyList());
	}

	@Test
	void 발송이_실패하면_등록을_유지하고_완료_이력을_남기지_않는다() {
		var cause = new IllegalStateException("FCM unavailable");
		var failure = new NotificationDeliveryException(cause);
		given(sender.send(anyList())).willThrow(failure);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isSameAs(failure).hasCause(cause);
		assertThat(fids.findByFid(fid)).isPresent();
		assertThat(dispatches.existsByScheduledAt(SCHEDULED_AT)).isFalse();
	}

	@Test
	void 발송이_실패해도_무효_FID_삭제는_유지한다() {
		NotificationSendResult failedWithInvalid = new NotificationSendResult(0, 1, List.of(fid), null);
		given(sender.send(anyList())).willReturn(failedWithInvalid);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isInstanceOf(NotificationDeliveryException.class);
		assertThat(fids.findByFid(fid)).isEmpty();
		assertThat(dispatches.existsByScheduledAt(SCHEDULED_AT)).isFalse();
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
		request = new ScheduledNotificationDispatchRequest(request.date(), 24, 0);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isInstanceOf(InvalidNotificationHourException.class);
		assertThat(dispatches.existsByScheduledAt(SCHEDULED_AT)).isFalse();
		verifyNoInteractions(sender);
	}

	@Test
	void 미래_예약은_발송하지_않는다() {
		given(clock.instant()).willReturn(SCHEDULED_AT.minusSeconds(1));

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isInstanceOf(InvalidNotificationDispatchTimeException.class);
		verifyNoInteractions(sender);
	}

	@Test
	void 첫_요청이_완료되면_기다리던_중복_요청은_발송하지_않는다() throws Exception {
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		given(sender.send(anyList())).willAnswer(invocation -> {
			started.countDown();
			assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			NotificationSendResult batchResult = results(List.of());
			return batchResult;
		});
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> dispatcher.dispatch(request));
			assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			var second = executor.submit(() -> dispatcher.dispatch(request));
			assertThatThrownBy(() -> second.get(500, java.util.concurrent.TimeUnit.MILLISECONDS))
					.isInstanceOf(java.util.concurrent.TimeoutException.class);
			release.countDown();

			assertThat(first.get(10, java.util.concurrent.TimeUnit.SECONDS).duplicate()).isFalse();
			assertThat(second.get(10, java.util.concurrent.TimeUnit.SECONDS).duplicate()).isTrue();
		}
		verify(sender, times(1)).send(anyList());
	}

	@Test
	void 첫_요청이_실패하면_기다리던_중복_요청이_발송한다() throws Exception {
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var failure = new NotificationDeliveryException(new IllegalStateException("FCM unavailable"));
		NotificationSendResult batchResult = results(List.of());
		given(sender.send(anyList())).willAnswer(invocation -> {
			started.countDown();
			assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			throw failure;
		}).willReturn(batchResult);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> dispatcher.dispatch(request));
			assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			var second = executor.submit(() -> dispatcher.dispatch(request));
			assertThatThrownBy(() -> second.get(500, java.util.concurrent.TimeUnit.MILLISECONDS))
					.isInstanceOf(java.util.concurrent.TimeoutException.class);
			release.countDown();

			assertThatThrownBy(() -> first.get(10, java.util.concurrent.TimeUnit.SECONDS)).hasCause(failure);
			assertThat(second.get(10, java.util.concurrent.TimeUnit.SECONDS).duplicate()).isFalse();
		}
		assertThat(dispatches.existsByScheduledAt(SCHEDULED_AT)).isTrue();
		verify(sender, times(2)).send(anyList());
	}

	private NotificationSendResult results(List<String> invalidFids) {
		return new NotificationSendResult(0, 0, invalidFids, null);
	}

}
