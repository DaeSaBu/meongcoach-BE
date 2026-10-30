package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationDispatchResponse;
import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationTargetResponse;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

	private final NotificationTargetFinder notificationTargetFinder;
	private final NotificationRegister notificationRegister;
	private final NotificationDispatcher dispatcher;

	@PostMapping("/dispatch")
	public NotificationDispatchResponse dispatch(@Valid @RequestBody NotificationDispatchRequest request) {
		var result = dispatcher.dispatch(request);
		return NotificationDispatchResponse.from(result);
	}

	@GetMapping("/targets")
	public List<NotificationTargetResponse> findTargets(@RequestParam int hour) {
		return notificationTargetFinder.findTargets(hour).stream()
				.map(NotificationTargetResponse::from)
				.toList();
	}

	@DeleteMapping("/installations")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDeviceInstallations(@Valid @RequestBody DeviceInstallationDeleteRequest request) {
		notificationRegister.deleteDeviceInstallations(request);
	}
}
