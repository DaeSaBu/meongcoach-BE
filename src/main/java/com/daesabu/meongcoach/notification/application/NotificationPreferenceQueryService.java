package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceFinder;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import com.daesabu.meongcoach.notification.domain.NotificationPreferences;
import com.daesabu.meongcoach.notification.domain.SlotPreference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferenceQueryService implements NotificationPreferenceFinder {

	private final NotificationPreferenceRepository notificationPreferenceRepository;

	@Override
	public List<SlotPreference> findSlotPreferences(Long userId) {
		List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUserId(userId);
		NotificationPreferences notificationPreferences = new NotificationPreferences(userId, preferences);
		return notificationPreferences.slotPreferences();
	}
}
