package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.required.DeviceTokenRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DeviceToken;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationTargetQueryService implements NotificationTargetFinder {

	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final DeviceTokenRepository deviceTokenRepository;
	private final UserFinder userFinder;

	@Override
	public List<NotificationTargetResult> findTargets(int hour) {
		NotificationHour.validate(hour);
		List<Long> enabledUserIds = notificationPreferenceRepository.findUserIdsByHourEnabled(hour);
		Set<Long> activeUserIds = userFinder.findActiveUserIds(enabledUserIds);
		if (activeUserIds.isEmpty()) {
			return List.of();
		}
		Map<Long, List<DeviceToken>> tokensByUserId = deviceTokenRepository.findAllByUserIdIn(activeUserIds).stream()
				.collect(Collectors.groupingBy(DeviceToken::getUserId, LinkedHashMap::new, Collectors.toList()));
		return tokensByUserId.entrySet().stream()
				.map(entry -> NotificationTargetResult.of(entry.getKey(), entry.getValue()))
				.toList();
	}
}
