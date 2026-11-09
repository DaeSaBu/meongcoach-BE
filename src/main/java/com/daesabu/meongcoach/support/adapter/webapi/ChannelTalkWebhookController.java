package com.daesabu.meongcoach.support.adapter.webapi;

import com.daesabu.meongcoach.support.application.provided.SupportReplyNotifier;
import com.daesabu.meongcoach.support.application.provided.dto.ChannelTalkWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChannelTalkWebhookController {

	private final SupportReplyNotifier supportReplyNotifier;

	@PostMapping("/webhooks/channel-talk/support-replies")
	public void receive(@RequestBody ChannelTalkWebhookRequest request) {
		supportReplyNotifier.notifyReply(request);
	}
}
