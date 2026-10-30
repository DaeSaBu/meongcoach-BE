package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationDispatchRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationDispatchService implements NotificationDispatcher {
	private static final Map<Integer, Map.Entry<String, String>> MESSAGES = Map.of(
			8, Map.entry("아침 훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?"),
			11, Map.entry("점심 훈련 시간이에요", "잠깐 짬을 내서 한 가지만 연습해 볼까요?"),
			17, Map.entry("저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?")
	);
	private static final Map.Entry<String, String> DEFAULT_MESSAGE = Map.entry("훈련 시간이에요", "오늘의 훈련을 시작해 볼까요?");

	private final NotificationDispatchRepository dispatches;
	private final NotificationPreferenceRepository preferences;
	private final DeviceInstallationRepository installations;
	private final UserFinder users;
	private final NotificationSender sender;

	@Override
	@Transactional
	public NotificationDispatchResult dispatch(NotificationDispatchRequest request) {
		NotificationHour.validate(request.hour());
		dispatches.lock(request.key());
		if (dispatches.existsByDispatchKey(request.key())) {
			return new NotificationDispatchResult(0, true);
		}
		List<String> fids = findTargets(request.hour());
		if (!fids.isEmpty()) {
			List<NotificationBatchResult> results = send(fids, request);
			List<String> invalidFids = results.stream().flatMap(result -> result.invalidFids().stream()).toList();
			deleteInvalidInstallations(invalidFids);
		}
		NotificationDispatch dispatch = NotificationDispatch.complete(request.key());
		dispatches.save(dispatch);
		return new NotificationDispatchResult(fids.size(), false);
	}

	private List<String> findTargets(int hour) {
		List<Long> enabledUserIds = preferences.findUserIdsByHourEnabled(hour);
		Set<Long> activeUserIds = users.findActiveUserIds(enabledUserIds);
		if (activeUserIds.isEmpty()) {
			return List.of();
		}
		return installations.findAllByUserIdInOrderByUserIdAscIdAsc(activeUserIds).stream()
				.map(DeviceInstallation::getFid).distinct().toList();
	}

	private List<NotificationBatchResult> send(List<String> fids, NotificationDispatchRequest request) {
		NotificationMessage message = message(request);
		Map<String, NotificationMessage> messages = new LinkedHashMap<>();
		fids.forEach(fid -> messages.put(fid, message));
		return sender.send(messages);
	}

	private NotificationMessage message(NotificationDispatchRequest request) {
		Map.Entry<String, String> content = MESSAGES.getOrDefault(request.hour(), DEFAULT_MESSAGE);
		Map<String, String> data = Map.of("hour", String.valueOf(request.hour()), "deepLink", "meongcoach://training-curriculum");
		return new NotificationMessage(content.getKey(), content.getValue(), data, request.key());
	}

	private void deleteInvalidInstallations(List<String> invalidFids) {
		if (invalidFids.isEmpty()) {
			return;
		}
		installations.deleteAllByFidIn(invalidFids);
	}
}
