package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.NotificationTarget;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationRecipientQueryService {

	private final NotificationScheduleRepository schedules;
	private final NotificationEventSettingRepository settings;
	private final DeviceInstallationRepository installations;
	private final UserFinder users;

	public List<String> findScheduledFids(NotificationTime time) {
		List<Long> userIds = schedules.findUserIdsByTimeEnabled(time);
		return findFids(userIds);
	}

	public List<String> findEventFids(NotificationEventType type, NotificationTarget target) {
		List<Long> userIds = findEnabledUserIds(type, target);
		return findFids(userIds);
	}

	private List<Long> findEnabledUserIds(NotificationEventType type, NotificationTarget target) {
		if (target.all()) {
			return settings.findUserIdsByTypeEnabled(type);
		}
		return settings.findUserIdsByTypeEnabledAndUserIdIn(type, target.userIds());
	}

	private List<String> findFids(List<Long> userIds) {
		Set<Long> activeUserIds = users.findActiveUserIds(userIds);
		if (activeUserIds.isEmpty()) {
			return List.of();
		}
		return installations.findAllByUserIdInOrderByUserIdAscIdAsc(activeUserIds).stream()
				.map(DeviceInstallation::getFid).distinct().toList();
	}
}
