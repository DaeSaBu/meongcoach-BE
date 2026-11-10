package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.application.provided.dto.ChannelTalkWebhookRequest;

public interface SupportReplyNotifier {
	void notifyReply(ChannelTalkWebhookRequest request);
}
