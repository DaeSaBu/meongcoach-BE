package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.NotificationDispatchResponse;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

	private final NotificationDispatcher dispatcher;

	@PostMapping("/dispatch")
	public NotificationDispatchResponse dispatch(@Valid @RequestBody NotificationDispatchRequest request) {
		var result = dispatcher.dispatch(request);
		return NotificationDispatchResponse.from(result);
	}

}
