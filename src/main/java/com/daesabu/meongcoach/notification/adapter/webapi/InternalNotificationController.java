package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.EventNotificationDispatchResponse;
import com.daesabu.meongcoach.notification.adapter.webapi.dto.ScheduledNotificationDispatchResponse;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
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

	private final ScheduledNotificationDispatcher scheduledDispatcher;
	private final EventNotificationDispatcher eventDispatcher;

	@PostMapping("/scheduled-dispatches")
	public ScheduledNotificationDispatchResponse dispatchScheduled(@Valid @RequestBody ScheduledNotificationDispatchRequest request) {
		var result = scheduledDispatcher.dispatch(request);
		return ScheduledNotificationDispatchResponse.from(result);
	}

	@PostMapping("/event-dispatches")
	public EventNotificationDispatchResponse dispatchEvent(@Valid @RequestBody EventNotificationDispatchRequest request) {
		var result = eventDispatcher.dispatch(request);
		return EventNotificationDispatchResponse.from(result);
	}
}
