package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.SupportChatSessionResponse;
import com.daesabu.meongcoach.notification.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.notification.application.provided.SupportReplyReceiver;
import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChannelTalkController {
	private final SupportReplyReceiver receiver;
	private final SupportChatSessionFinder sessions;

	@PostMapping("/webhooks/channel-talk")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void receive(@RequestParam(required = false) String token, @RequestBody ChannelTalkWebhookRequest request) {
		receiver.receive(token, request);
	}

	@GetMapping("/api/notifications/support-session")
	public ResponseEntity<SupportChatSessionResponse> session(@CurrentUserId Long userId) {
		SupportChatSessionResult session = sessions.find(userId);
		SupportChatSessionResponse response = new SupportChatSessionResponse(session.memberId(), session.memberHash());
		ResponseEntity<SupportChatSessionResponse> result = ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
		return result;
	}
}
