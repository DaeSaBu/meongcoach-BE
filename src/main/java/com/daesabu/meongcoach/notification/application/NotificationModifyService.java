package com.daesabu.meongcoach.notification.application;


import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
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

	private final DeviceInstallationRepository deviceInstallationRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;

	@Override
	@Transactional
	public void registerDeviceInstallation(Long userId, DeviceInstallationRegisterRequest request) {
		DeviceInstallationRegisterCommand command = request.toCommand();
		Instant now = Instant.now();

		upsertDeviceInstallation(userId, command, now);
		createDefaultPreferencesIfAbsent(userId);
	}

	private void upsertDeviceInstallation(Long userId, DeviceInstallationRegisterCommand command, Instant now) {
		Optional<DeviceInstallation> existing = deviceInstallationRepository.findByFid(command.fid());
		if (existing.isPresent()) {
			existing.get().renew(userId, now);
			return;
		}
		deviceInstallationRepository.save(DeviceInstallation.register(userId, command, now));
	}

	private void createDefaultPreferencesIfAbsent(Long userId) {
		List<Integer> existingHours = notificationPreferenceRepository.findAllByUserId(userId).stream()
				.map(NotificationPreference::getHour)
				.toList();
		List<NotificationPreference> missingPreferences = NotificationPreference.createDefaults(userId).stream()
				.filter(preference -> !existingHours.contains(preference.getHour()))
				.toList();
		notificationPreferenceRepository.saveAll(missingPreferences);
	}

	@Override
	@Transactional
	public void deleteDeviceInstallation(Long userId, DeviceInstallationUnregisterRequest request) {
		deviceInstallationRepository.deleteByUserIdAndFid(userId, request.fid());
	}

	@Override
	@Transactional
	public void deleteDeviceInstallations(DeviceInstallationDeleteRequest request) {
		if (request.fids().isEmpty()) {
			return;
		}
		deviceInstallationRepository.deleteAllByFidIn(request.fids());
	}

	@Override
	@Transactional
	public void updatePreference(Long userId, int hour, NotificationPreferenceUpdateRequest request) {
		NotificationHour.validateAllowed(hour);
		boolean enabled = request.enabled();

		Optional<NotificationPreference> existing = notificationPreferenceRepository.findByUserIdAndHour(userId, hour);
		if (existing.isPresent()) {
			existing.get().change(enabled);
			return;
		}
		notificationPreferenceRepository.save(NotificationPreference.create(userId, hour, enabled));
	}

	@Override
	@Transactional
	public void deleteAllByUserId(Long userId) {
		deviceInstallationRepository.deleteAllByUserId(userId);
		notificationPreferenceRepository.deleteAllByUserId(userId);
	}
}
