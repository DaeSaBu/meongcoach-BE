package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.domain.NotificationSchedule;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationScheduleFinder {

	private final NotificationScheduleRepository notificationScheduleRepository;

	@Override
	public List<NotificationScheduleResult> findSchedules(Long userId) {
		List<NotificationSchedule> schedules = notificationScheduleRepository.findAllByUserId(userId);
		return schedules.stream()
				.sorted(Comparator.comparingInt(NotificationSchedule::getHour))
				.map(NotificationScheduleResult::from)
				.toList();
	}
}
