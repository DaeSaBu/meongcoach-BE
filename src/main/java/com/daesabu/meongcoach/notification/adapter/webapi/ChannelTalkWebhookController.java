package com.daesabu.meongcoach.notification.adapter.webapi;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.ChannelTalkWebhookPayload;
import com.daesabu.meongcoach.notification.application.ChannelTalkProperties;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChannelTalkWebhookController {

	private static final String EVENT_TYPE = "SUPPORT_REPLY";
	private static final String TARGET_KIND = "USERS";
	private static final String TITLE = "문의하신 내용에 답변이 왔어요";
	private static final String DEEP_LINK = "meongcoach://support-chat";

	private final EventNotificationDispatcher eventDispatcher;
	private final ChannelTalkProperties properties;

	@PostMapping("/webhooks/channel-talk")
	public void receive(@RequestParam(required = false) String token, @RequestBody ChannelTalkWebhookPayload payload) {
		if (!properties.matches(token)) {
			throw new BadCredentialsException("채널톡 웹훅 토큰이 일치하지 않습니다.");
		}
		payload.recipientUserId()
				.ifPresent(userId -> eventDispatcher.dispatch(new EventNotificationDispatchRequest(EVENT_TYPE, TARGET_KIND,
						List.of(userId), TITLE, payload.notificationBody(), DEEP_LINK)));
	}
}
