package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.SupportChatSessionResponse;
import com.daesabu.meongcoach.notification.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SupportChatSessionController {
	private final SupportChatSessionFinder supportChatSessionFinder;

	@GetMapping("/api/notifications/support-session")
	public SupportChatSessionResponse session(@CurrentUserId Long userId) {
		SupportChatSessionResult session = supportChatSessionFinder.find(userId);
		return SupportChatSessionResponse.from(session);
	}
}
