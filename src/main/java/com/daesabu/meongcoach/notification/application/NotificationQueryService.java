package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceResult;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationPreferenceFinder {

	private final NotificationPreferenceRepository notificationPreferenceRepository;

	@Override
	public List<NotificationPreferenceResult> findPreferences(Long userId) {
		List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUserId(userId);
		return preferences.stream()
				.sorted(Comparator.comparingInt(NotificationPreference::getHour))
				.map(NotificationPreferenceResult::from)
				.toList();
	}
}
