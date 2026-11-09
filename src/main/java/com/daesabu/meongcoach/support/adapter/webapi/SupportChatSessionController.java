package com.daesabu.meongcoach.support.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.support.adapter.webapi.dto.SupportChatSessionResponse;
import com.daesabu.meongcoach.support.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.support.application.provided.dto.SupportChatSessionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SupportChatSessionController {
	private final SupportChatSessionFinder supportChatSessionFinder;

	@GetMapping("/api/support/session")
	public SupportChatSessionResponse session(@CurrentUserId Long userId) {
		SupportChatSessionResult session = supportChatSessionFinder.find(userId);
		return SupportChatSessionResponse.from(session);
	}
}
