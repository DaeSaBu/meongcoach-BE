package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;

public interface SupportReplyNotifier {
	void notifyReply(ChannelTalkWebhookRequest request);
}
