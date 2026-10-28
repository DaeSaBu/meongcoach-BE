package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceTokenRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DeviceToken;
import com.daesabu.meongcoach.notification.domain.DeviceTokenRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceUpdateCommand;
import com.daesabu.meongcoach.notification.domain.NotificationPreferences;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationModifyService implements NotificationRegister {

	private final DeviceTokenRepository deviceTokenRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;

	@Override
	@Transactional
	public void registerDeviceToken(Long userId, DeviceTokenRegisterRequest request) {
		DeviceTokenRegisterCommand command = request.toCommand();
		Instant now = Instant.now();

		upsertDeviceToken(userId, command, now);
		createDefaultPreferencesIfAbsent(userId);
	}

	private void upsertDeviceToken(Long userId, DeviceTokenRegisterCommand command, Instant now) {
		Optional<DeviceToken> existing = deviceTokenRepository.findByToken(command.token());
		if (existing.isPresent()) {
			existing.get().renew(userId, now);
			return;
		}
		deviceTokenRepository.save(DeviceToken.register(userId, command, now));
	}

	private void createDefaultPreferencesIfAbsent(Long userId) {
		NotificationPreferences preferences = loadPreferences(userId);
		if (!preferences.isEmpty()) {
			return;
		}
		notificationPreferenceRepository.saveAll(preferences.createDefaults());
	}

	@Override
	@Transactional
	public void deleteDeviceToken(Long userId, String token) {
		deviceTokenRepository.deleteByUserIdAndToken(userId, token);
	}

	@Override
	@Transactional
	public void deleteDeviceTokens(DeviceTokenDeleteRequest request) {
		if (request.tokens().isEmpty()) {
			return;
		}
		deviceTokenRepository.deleteAllByTokenIn(request.tokens());
	}

	@Override
	@Transactional
	public void updatePreferences(Long userId, NotificationPreferenceUpdateRequest request) {
		NotificationPreferenceUpdateCommand command = request.toCommand();
		NotificationPreferences preferences = loadPreferences(userId);

		List<NotificationPreference> created = preferences.apply(command);
		notificationPreferenceRepository.saveAll(created);
	}

	@Override
	@Transactional
	public void deleteAllByUserId(Long userId) {
		deviceTokenRepository.deleteAllByUserId(userId);
		notificationPreferenceRepository.deleteAllByUserId(userId);
	}

	private NotificationPreferences loadPreferences(Long userId) {
		List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUserId(userId);
		return new NotificationPreferences(userId, preferences);
	}
}
