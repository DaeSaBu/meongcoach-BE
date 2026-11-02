package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationSchedule;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationScheduleFinder, NotificationEventSettingFinder {

	private final NotificationScheduleRepository notificationScheduleRepository;
	private final NotificationEventSettingRepository notificationEventSettingRepository;

	@Override
	public List<NotificationScheduleResult> findSchedules(Long userId) {
		List<NotificationSchedule> schedules = notificationScheduleRepository.findAllByUserId(userId);
		return schedules.stream()
				.sorted(Comparator.comparingInt(NotificationSchedule::getHour).thenComparingInt(NotificationSchedule::getMinute))
				.map(NotificationScheduleResult::from)
				.toList();
	}

	@Override
	public List<NotificationEventSettingResult> findEventSettings(Long userId) {
		List<NotificationEventSetting> settings = notificationEventSettingRepository.findAllByUserId(userId);
		return settings.stream()
				.sorted(Comparator.comparing(NotificationEventSetting::getType))
				.map(NotificationEventSettingResult::from)
				.toList();
	}
}
