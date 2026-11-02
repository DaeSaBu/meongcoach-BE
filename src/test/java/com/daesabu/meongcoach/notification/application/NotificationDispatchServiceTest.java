package com.daesabu.meongcoach.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationSendResult;
import com.daesabu.meongcoach.notification.application.required.ScheduledNotificationDispatchRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationDeepLink;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatch;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationDispatchServiceTest {
	private final ScheduledNotificationDispatchRepository dispatches = mock(ScheduledNotificationDispatchRepository.class);
	private final NotificationScheduleRepository schedules = mock(NotificationScheduleRepository.class);
	private final UserFinder users = mock(UserFinder.class);
	private final DeviceInstallationRepository installations = mock(DeviceInstallationRepository.class);
	private final NotificationSender sender = mock(NotificationSender.class);
	private final NotificationEventSettingRepository settings = mock(NotificationEventSettingRepository.class);
	private final NotificationDispatchService service = new NotificationDispatchService(
			dispatches, schedules, settings, installations, new DeviceInstallationCleanupService(installations), users, sender, Clock.fixed(Instant.parse("2026-10-31T08:00:00Z"), ZoneOffset.UTC));
	private final ScheduledNotificationDispatchRequest request = new ScheduledNotificationDispatchRequest(LocalDate.of(2026, 10, 31), 17, 0);

	@Test
	void 시각별_메시지를_구성하고_전체_대상을_Sender에_전달한다() {
		prepareTargets(501);
		NotificationSendResult deliveryResults = results(List.of());
		given(sender.send(anyList())).willReturn(deliveryResults);

		assertThat(service.dispatch(request).fids()).isEqualTo(501);

		ArgumentCaptor<List<PushNotification>> messages = ArgumentCaptor.captor();
		verify(sender, times(1)).send(messages.capture());
		assertThat(messages.getValue()).hasSize(501);
		PushNotification message = messages.getValue().getFirst();
		assertThat(message.title()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(message.body()).isEqualTo("오늘의 훈련을 마무리해 볼까요?");
		assertThat(message.hour()).isEqualTo(17);
		assertThat(message.deepLink()).isEqualTo(new NotificationDeepLink("meongcoach://training-curriculum"));
		assertThat(message.collapseKey()).isEqualTo("2026-10-31T08:00:00Z");
		verify(dispatches).save(any(ScheduledNotificationDispatch.class));
	}

	@Test
	void 여러_배치의_무효_FID을_모아_삭제한다() {
		prepareTargets(1001);
		NotificationSendResult result = results(List.of("fid-1", "fid-500"));
		given(sender.send(anyList())).willReturn(result);

		assertThat(service.dispatch(request).fids()).isEqualTo(1001);

		verify(sender, times(1)).send(anyList());
		verify(installations).deleteAllByFidIn(List.of("fid-1", "fid-500"));
		verify(dispatches).save(any(ScheduledNotificationDispatch.class));
	}

	@Test
	void 전송에_성공한_FID이_없고_무효_외_실패가_있으면_완료_이력을_남기지_않고_던진다() {
		prepareTargets(2);
		NotificationSendResult allFailed = results(0, 2, List.of());
		given(sender.send(anyList())).willReturn(allFailed);

		assertThatThrownBy(() -> service.dispatch(request)).isInstanceOf(NotificationDeliveryException.class);

		verify(dispatches, never()).save(any());
	}

	@Test
	void 무효_FID만_있으면_삭제하고_완료한다() {
		prepareTargets(2);
		NotificationSendResult onlyInvalid = results(0, 0, List.of("fid-0", "fid-1"));
		given(sender.send(anyList())).willReturn(onlyInvalid);

		assertThat(service.dispatch(request).fids()).isEqualTo(2);

		verify(installations).deleteAllByFidIn(List.of("fid-0", "fid-1"));
		verify(dispatches).save(any(ScheduledNotificationDispatch.class));
	}

	@Test
	void 배치_호출이_실패하면_원인과_함께_던지고_완료_이력을_남기지_않는다() {
		prepareTargets(1001);
		var cause = new IllegalStateException("FCM unavailable");
		NotificationSendResult result = new NotificationSendResult(499, 500, List.of("fid-1"), cause);
		given(sender.send(anyList())).willReturn(result);

		assertThatThrownBy(() -> service.dispatch(request)).isInstanceOf(NotificationDeliveryException.class).hasCause(cause);

		verify(dispatches, never()).save(any());
	}

	private void prepareTargets(int count) {
		given(schedules.findUserIdsByTimeEnabled(new NotificationTime(17, 0))).willReturn(List.of(1L));
		given(users.findActiveUserIds(List.of(1L))).willReturn(Set.of(1L));
		List<DeviceInstallation> results = IntStream.range(0, count)
				.mapToObj(index -> DeviceInstallation.register(1L,
						new DeviceInstallationRegisterCommand("fid-" + index, DevicePlatform.IOS), Instant.parse("2026-10-31T08:00:00Z"))).toList();
		given(installations.findAllByUserIdInOrderByUserIdAscIdAsc(Set.of(1L))).willReturn(results);
	}
	private NotificationSendResult results(List<String> invalidFids) {
		return results(0, 0, invalidFids);
	}

	private NotificationSendResult results(int sent, int failed, List<String> invalidFids) {
		return new NotificationSendResult(sent, failed, invalidFids, null);
	}

}
