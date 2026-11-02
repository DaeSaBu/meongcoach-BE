package com.daesabu.meongcoach.notification.application;


import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationEventSettingUpdateRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationScheduleUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationEventSettings;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.NotificationSchedule;
import com.daesabu.meongcoach.notification.domain.NotificationSchedules;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationRegisterService implements NotificationRegister {

	private final DeviceInstallationRepository deviceInstallationRepository;
	private final NotificationScheduleRepository notificationScheduleRepository;
	private final NotificationEventSettingRepository notificationEventSettingRepository;

	@Override
	@Transactional
	public void registerDeviceInstallation(Long userId, DeviceInstallationRegisterRequest request) {
		DeviceInstallationRegisterCommand command = request.toCommand();
		Instant now = Instant.now();

		upsertDeviceInstallation(userId, command, now);
		createDefaultSchedulesIfAbsent(userId);
		createDefaultEventSettingsIfAbsent(userId);
	}

	private void upsertDeviceInstallation(Long userId, DeviceInstallationRegisterCommand command, Instant now) {
		Optional<DeviceInstallation> existing = deviceInstallationRepository.findByFid(command.fid());
		if (existing.isPresent()) {
			existing.get().renew(userId, now);
			return;
		}
		deviceInstallationRepository.save(DeviceInstallation.register(userId, command, now));
	}

	private void createDefaultSchedulesIfAbsent(Long userId) {
		NotificationSchedules schedules = new NotificationSchedules(notificationScheduleRepository.findAllByUserId(userId));
		notificationScheduleRepository.saveAll(schedules.createMissingDefaults(userId));
	}

	private void createDefaultEventSettingsIfAbsent(Long userId) {
		NotificationEventSettings settings = new NotificationEventSettings(notificationEventSettingRepository.findAllByUserId(userId));
		notificationEventSettingRepository.saveAll(settings.createMissingDefaults(userId));
	}

	@Override
	@Transactional
	public void deleteDeviceInstallation(Long userId, DeviceInstallationUnregisterRequest request) {
		deviceInstallationRepository.deleteByUserIdAndFid(userId, request.fid());
	}

	@Override
	@Transactional
	public void updateSchedule(Long userId, NotificationScheduleUpdateRequest request) {
		NotificationTime time = new NotificationTime(request.hour(), request.minute());
		time.validateAllowed();
		boolean enabled = request.enabled();

		Optional<NotificationSchedule> existing = notificationScheduleRepository.findByUserIdAndHourAndMinute(userId, time.hour(), time.minute());
		if (existing.isPresent()) {
			existing.get().change(enabled);
			return;
		}
		notificationScheduleRepository.save(NotificationSchedule.create(userId, time, enabled));
	}

	@Override
	@Transactional
	public void updateEventSetting(Long userId, NotificationEventSettingUpdateRequest request) {
		NotificationEventType type = NotificationEventType.from(request.type());
		boolean enabled = request.enabled();

		Optional<NotificationEventSetting> existing = notificationEventSettingRepository.findByUserIdAndType(userId, type);
		if (existing.isPresent()) {
			existing.get().change(enabled);
			return;
		}
		notificationEventSettingRepository.save(NotificationEventSetting.create(userId, type, enabled));
	}

	@Override
	@Transactional
	public void deleteAllByUserId(Long userId) {
		deviceInstallationRepository.deleteAllByUserId(userId);
		notificationScheduleRepository.deleteAllByUserId(userId);
		notificationEventSettingRepository.deleteAllByUserId(userId);
	}
}
