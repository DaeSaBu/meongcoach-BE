package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationPreferencesResponse;
import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final NotificationPreferenceFinder notificationPreferenceFinder;

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

	@GetMapping("/preferences")
	public NotificationPreferencesResponse findPreferences(@CurrentUserId Long userId) {
		List<NotificationPreferenceResult> preferences = notificationPreferenceFinder.findPreferences(userId);
		return NotificationPreferencesResponse.from(preferences);
	}

	@PutMapping("/preferences/{hour}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updatePreference(@CurrentUserId Long userId, @PathVariable int hour,
	                             @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
		notificationRegister.updatePreference(userId, hour, request);
	}
}
