package com.daesabu.meongcoach.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationBatchResult;
import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationDispatchServiceTest {
	private final NotificationDispatchRepository dispatches = mock(NotificationDispatchRepository.class);
	private final NotificationTargetFinder targets = mock(NotificationTargetFinder.class);
	private final DeviceInstallationRepository installations = mock(DeviceInstallationRepository.class);
	private final NotificationSender sender = mock(NotificationSender.class);
	private final NotificationDispatchService service = new NotificationDispatchService(dispatches, targets, installations, sender);
	private final NotificationDispatchRequest request = new NotificationDispatchRequest(Instant.parse("2026-10-31T08:00:00Z"), 17);

	@Test
	void 시각별_메시지를_구성하고_전체_대상을_Sender에_전달한다() {
		prepareTargets(501);
		NotificationBatchResult deliveryResults = results(List.of(), null);
		given(sender.send(anyList(), any())).willReturn(List.of(deliveryResults));

		assertThat(service.dispatch(request).fids()).isEqualTo(501);

		ArgumentCaptor<List<String>> batches = ArgumentCaptor.captor();
		ArgumentCaptor<NotificationMessage> messages = ArgumentCaptor.forClass(NotificationMessage.class);
		verify(sender, times(1)).send(batches.capture(), messages.capture());
		assertThat(batches.getAllValues()).extracting(List::size).containsExactly(501);
		assertThat(messages.getValue().title()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(messages.getValue().body()).isEqualTo("오늘의 훈련을 마무리해 볼까요?");
		assertThat(messages.getValue().data()).isEqualTo(Map.of("hour", "17", "deepLink", "meongcoach://training-curriculum"));
		verify(dispatches).save(any(NotificationDispatch.class));
	}

	@Test
	void 전체_응답에서_무효_FID을_삭제하고_첫_실패를_전파한다() {
		prepareTargets(1001);
		var first = new NotificationDeliveryException(new IllegalStateException("first"));
		NotificationBatchResult firstBatch = results(List.of("fid-1"), first);
		NotificationBatchResult secondBatch = results(List.of("fid-500"), null);
		given(sender.send(anyList(), any())).willReturn(List.of(firstBatch, secondBatch));

		assertThatThrownBy(() -> service.dispatch(request)).isSameAs(first);

		verify(sender, times(1)).send(anyList(), any());
		verify(installations).deleteAllByFidIn(List.of("fid-1", "fid-500"));
		verify(dispatches, never()).save(any());
	}

	private void prepareTargets(int count) {
		given(dispatches.tryLock(request.key())).willReturn(true);
		List<NotificationTargetResult> results = IntStream.range(0, count)
				.mapToObj(index -> new NotificationTargetResult(1L, "fid-" + index, DevicePlatform.IOS)).toList();
		given(targets.findTargets(17)).willReturn(results);
	}
	private NotificationBatchResult results(List<String> invalidFids, NotificationDeliveryException failure) {
		NotificationBatchResult results = mock(NotificationBatchResult.class);
		given(results.invalidFids()).willReturn(invalidFids);
		if (failure != null) {
			willThrow(failure).given(results).throwIfFailed();
		}
		return results;
	}

}
