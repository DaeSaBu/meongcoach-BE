package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;

public interface SupportReplyReceiver {
	void receive(String token, ChannelTalkWebhookRequest request);
}
