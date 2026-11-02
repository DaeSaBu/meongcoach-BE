package com.daesabu.meongcoach.notification.application;


import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationScheduleUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.notification.domain.NotificationSchedule;
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
public class NotificationRegisterService implements NotificationRegister {

	private final DeviceInstallationRepository deviceInstallationRepository;
	private final NotificationScheduleRepository notificationScheduleRepository;

	@Override
	@Transactional
	public void registerDeviceInstallation(Long userId, DeviceInstallationRegisterRequest request) {
		DeviceInstallationRegisterCommand command = request.toCommand();
		Instant now = Instant.now();

		upsertDeviceInstallation(userId, command, now);
		createDefaultSchedulesIfAbsent(userId);
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
		List<Integer> existingHours = notificationScheduleRepository.findAllByUserId(userId).stream()
				.map(NotificationSchedule::getHour)
				.toList();
		List<NotificationSchedule> missingSchedules = NotificationSchedule.createDefaults(userId).stream()
				.filter(schedule -> !existingHours.contains(schedule.getHour()))
				.toList();
		notificationScheduleRepository.saveAll(missingSchedules);
	}

	@Override
	@Transactional
	public void deleteDeviceInstallation(Long userId, DeviceInstallationUnregisterRequest request) {
		deviceInstallationRepository.deleteByUserIdAndFid(userId, request.fid());
	}

	@Override
	@Transactional
	public void updateSchedule(Long userId, int hour, NotificationScheduleUpdateRequest request) {
		NotificationHour.validateAllowed(hour);
		boolean enabled = request.enabled();

		Optional<NotificationSchedule> existing = notificationScheduleRepository.findByUserIdAndHour(userId, hour);
		if (existing.isPresent()) {
			existing.get().change(enabled);
			return;
		}
		notificationScheduleRepository.save(NotificationSchedule.create(userId, hour, enabled));
	}

	@Override
	@Transactional
	public void deleteAllByUserId(Long userId) {
		deviceInstallationRepository.deleteAllByUserId(userId);
		notificationScheduleRepository.deleteAllByUserId(userId);
	}
}
