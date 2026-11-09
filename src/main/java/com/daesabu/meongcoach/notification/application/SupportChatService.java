package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.notification.application.provided.SupportReplyReceiver;
import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;
import com.daesabu.meongcoach.notification.application.required.SupportReplyReceiptRepository;
import com.daesabu.meongcoach.notification.domain.SupportReply;
import com.daesabu.meongcoach.notification.domain.SupportReplyReceipt;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportChatService implements SupportReplyReceiver, SupportChatSessionFinder {
	private final ChannelTalkProperties properties;
	private final SupportReplyReceiptRepository receipts;
	private final EventNotificationDispatcher dispatcher;
	private final Clock clock;

	@Override
	public SupportChatSessionResult find(Long userId) {
		String memberId = properties.environment() + "-" + userId;
		String memberHash = properties.memberHash(memberId);
		return new SupportChatSessionResult(memberId, memberHash);
	}

	@Override
	@Transactional
	public void receive(String token, ChannelTalkWebhookRequest request) {
		properties.verifyToken(token);
		Optional<SupportReply> reply = request.reply(properties.environment());
		if (reply.isEmpty()) {
			return;
		}
		SupportReply target = reply.get();
		receipts.lock(target.messageId());
		if (receipts.existsById(target.messageId())) {
			return;
		}
		EventNotificationDispatchRequest notification = new EventNotificationDispatchRequest(
				"SUPPORT_REPLY", "USERS", List.of(target.userId()), "문의 답변이 도착했어요", "문의하신 내용에 답변이 왔어요. 확인해 주세요.", "meongcoach://support-chat");
		dispatcher.dispatch(notification);
		receipts.save(SupportReplyReceipt.received(target.messageId(), clock.instant()));
	}
}
