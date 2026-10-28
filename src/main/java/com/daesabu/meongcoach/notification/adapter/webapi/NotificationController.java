package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationPreferencesResponse;
import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.domain.HourPreference;
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

	@PutMapping("/tokens")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registerDeviceToken(@CurrentUserId Long userId, @Valid @RequestBody DeviceTokenRegisterRequest request) {
		notificationRegister.registerDeviceToken(userId, request);
	}

	@DeleteMapping("/tokens/{token}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDeviceToken(@CurrentUserId Long userId, @PathVariable String token) {
		notificationRegister.deleteDeviceToken(userId, token);
	}

	@GetMapping("/preferences")
	public NotificationPreferencesResponse findPreferences(@CurrentUserId Long userId) {
		List<HourPreference> hours = notificationPreferenceFinder.findHourPreferences(userId);
		return NotificationPreferencesResponse.from(hours);
	}

	@PutMapping("/preferences")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updatePreferences(@CurrentUserId Long userId,
	                              @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
		notificationRegister.updatePreferences(userId, request);
	}
}
