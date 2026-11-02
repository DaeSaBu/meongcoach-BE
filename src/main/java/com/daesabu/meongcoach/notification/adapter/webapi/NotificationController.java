package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationEventSettingsResponse;
import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationSchedulesResponse;
import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationEventSettingUpdateRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationScheduleUpdateRequest;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationRegister notificationRegister;
	private final NotificationScheduleFinder notificationScheduleFinder;
	private final NotificationEventSettingFinder notificationEventSettingFinder;

	@PutMapping("/installations")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registerDeviceInstallation(@CurrentUserId Long userId, @Valid @RequestBody DeviceInstallationRegisterRequest request) {
		notificationRegister.registerDeviceInstallation(userId, request);
	}

	@DeleteMapping("/installations")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDeviceInstallation(@CurrentUserId Long userId, @Valid @RequestBody DeviceInstallationUnregisterRequest request) {
		notificationRegister.deleteDeviceInstallation(userId, request);
	}

	@GetMapping("/schedules")
	public NotificationSchedulesResponse findSchedules(@CurrentUserId Long userId) {
		List<NotificationScheduleResult> schedules = notificationScheduleFinder.findSchedules(userId);
		return NotificationSchedulesResponse.from(schedules);
	}

	@PutMapping("/schedules")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateSchedule(@CurrentUserId Long userId, @Valid @RequestBody NotificationScheduleUpdateRequest request) {
		notificationRegister.updateSchedule(userId, request);
	}

	@GetMapping("/event-settings")
	public NotificationEventSettingsResponse findEventSettings(@CurrentUserId Long userId) {
		List<NotificationEventSettingResult> settings = notificationEventSettingFinder.findEventSettings(userId);
		return new NotificationEventSettingsResponse(settings);
	}

	@PutMapping("/event-settings")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateEventSetting(@CurrentUserId Long userId, @Valid @RequestBody NotificationEventSettingUpdateRequest request) {
		notificationRegister.updateEventSetting(userId, request);
	}
}
