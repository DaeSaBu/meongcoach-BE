package com.daesabu.meongcoach.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.BatchResult;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationDispatchServiceTest {
	private final NotificationDispatchRepository dispatches = mock(NotificationDispatchRepository.class);
	private final NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
	private final UserFinder users = mock(UserFinder.class);
	private final DeviceInstallationRepository installations = mock(DeviceInstallationRepository.class);
	private final NotificationSender sender = mock(NotificationSender.class);
	private final NotificationDispatchService service = new NotificationDispatchService(dispatches, preferences, installations, users, sender);
	private final NotificationDispatchRequest request = new NotificationDispatchRequest(Instant.parse("2026-10-31T08:00:00Z"), 17);

	@Test
	void 시각별_메시지를_구성하고_전체_대상을_Sender에_전달한다() {
		prepareTargets(501);
		BatchResult deliveryResults = results(List.of());
		given(sender.send(anyMap())).willReturn(List.of(deliveryResults));

		assertThat(service.dispatch(request).fids()).isEqualTo(501);

		ArgumentCaptor<Map<String, NotificationMessage>> messages = ArgumentCaptor.captor();
		verify(sender, times(1)).send(messages.capture());
		assertThat(messages.getValue()).hasSize(501);
		NotificationMessage message = messages.getValue().values().iterator().next();
		assertThat(message.title()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(message.body()).isEqualTo("오늘의 훈련을 마무리해 볼까요?");
		assertThat(message.data()).isEqualTo(Map.of("hour", "17", "deepLink", "meongcoach://training-curriculum"));
		verify(dispatches).save(any(NotificationDispatch.class));
	}

	@Test
	void 여러_배치의_무효_FID을_모아_삭제한다() {
		prepareTargets(1001);
		BatchResult firstBatch = results(List.of("fid-1"));
		BatchResult secondBatch = results(List.of("fid-500"));
		given(sender.send(anyMap())).willReturn(List.of(firstBatch, secondBatch));

		assertThat(service.dispatch(request).fids()).isEqualTo(1001);

		verify(sender, times(1)).send(anyMap());
		verify(installations).deleteAllByFidIn(List.of("fid-1", "fid-500"));
		verify(dispatches).save(any(NotificationDispatch.class));
	}

	private void prepareTargets(int count) {
		given(dispatches.tryLock(request.key())).willReturn(true);
		given(preferences.findUserIdsByHourEnabled(17)).willReturn(List.of(1L));
		given(users.findActiveUserIds(List.of(1L))).willReturn(Set.of(1L));
		List<DeviceInstallation> results = IntStream.range(0, count)
				.mapToObj(index -> DeviceInstallation.register(1L,
						new DeviceInstallationRegisterCommand("fid-" + index, DevicePlatform.IOS), request.scheduledTime())).toList();
		given(installations.findAllByUserIdInOrderByUserIdAscIdAsc(Set.of(1L))).willReturn(results);
	}
	private BatchResult results(List<String> invalidFids) {
		BatchResult results = mock(BatchResult.class);
		given(results.invalidFids()).willReturn(invalidFids);
		return results;
	}

}
