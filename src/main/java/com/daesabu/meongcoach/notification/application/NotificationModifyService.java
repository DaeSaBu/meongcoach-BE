package com.daesabu.meongcoach.notification.application;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
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
		if (!notificationPreferenceRepository.findAllByUserId(userId).isEmpty()) {
			return;
		}
		List<NotificationPreference> defaults = NotificationPreference.createDefaults(userId);
		try {
			notificationPreferenceRepository.saveAll(defaults);
		} catch (DataIntegrityViolationException e) {
			log.info("기본 알림 설정이 이미 만들어져 있어 건너뜁니다. userId={}", userId);
		}
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
		Map<Integer, NotificationPreference> existingByHour = findPreferencesByHour(userId);

		List<NotificationPreference> created = new ArrayList<>();
		for (int hour : command.enabledByHour().keySet()) {
			boolean enabled = command.enabledFor(hour);
			NotificationPreference existing = existingByHour.get(hour);
			if (existing != null) {
				existing.change(enabled);
				continue;
			}
			created.add(NotificationPreference.create(userId, hour, enabled));
		}
		notificationPreferenceRepository.saveAll(created);
	}

	private Map<Integer, NotificationPreference> findPreferencesByHour(Long userId) {
		List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUserId(userId);
		return preferences.stream().collect(toMap(NotificationPreference::getHour, identity()));
	}

	@Override
	@Transactional
	public void deleteAllByUserId(Long userId) {
		deviceTokenRepository.deleteAllByUserId(userId);
		notificationPreferenceRepository.deleteAllByUserId(userId);
	}
}
