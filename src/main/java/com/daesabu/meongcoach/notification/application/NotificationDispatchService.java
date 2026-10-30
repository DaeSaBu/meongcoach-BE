package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.NotificationDeliveryResult;
import com.daesabu.meongcoach.notification.domain.NotificationDeliveryResults;
import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationDispatchService implements NotificationDispatcher {
	private static final Map<Integer, NotificationMessage> MESSAGES = Map.of(
			8, message(8, "아침 훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"),
			11, message(11, "점심 훈련 시간이에요", "잠깐 짬을 내서 한 가지만 연습해 볼까요?"),
			17, message(17, "저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?")
	);

	private final NotificationDispatchRepository dispatches;
	private final NotificationTargetFinder targets;
	private final DeviceInstallationRepository installations;
	private final NotificationSender sender;

	@Override
	@Transactional
	public NotificationDispatchResult dispatch(NotificationDispatchRequest request) {
		NotificationHour.validate(request.hour());
		if (!dispatches.tryLock(request.key()) || dispatches.existsByDispatchKey(request.key())) {
			return new NotificationDispatchResult(0, true);
		}
		List<String> fids = targets.findTargets(request.hour()).stream()
				.map(NotificationTargetResult::fid).distinct().toList();
		List<NotificationDeliveryResult> responses = send(fids, request.hour());
		NotificationDeliveryResults results = new NotificationDeliveryResults(responses);
		deleteInvalidInstallations(results.invalidFids());
		results.throwIfFailed();
		NotificationDispatch dispatch = NotificationDispatch.complete(request.key());
		dispatches.save(dispatch);
		return new NotificationDispatchResult(fids.size(), false);
	}

	private List<NotificationDeliveryResult> send(List<String> fids, int hour) {
		if (fids.isEmpty()) {
			return List.of();
		}
		NotificationMessage message = MESSAGES.getOrDefault(hour, message(hour, "훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"));
		return sender.send(fids, message);
	}

	private static NotificationMessage message(int hour, String title, String body) {
		Map<String, String> data = Map.of("hour", String.valueOf(hour), "deepLink", "meongcoach://training-curriculum");
		return new NotificationMessage(title, body, data);
	}

	private void deleteInvalidInstallations(List<String> invalidFids) {
		if (invalidFids.isEmpty()) {
			return;
		}
		installations.deleteAllByFidIn(invalidFids);
	}
}
