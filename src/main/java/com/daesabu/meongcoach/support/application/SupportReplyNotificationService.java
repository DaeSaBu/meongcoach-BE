package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.support.application.provided.SupportReplyNotifier;
import com.daesabu.meongcoach.support.application.provided.dto.ChannelTalkWebhookRequest;
import com.daesabu.meongcoach.support.domain.SupportReply;
import com.daesabu.meongcoach.support.domain.SupportReplyCommand;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportReplyNotificationService implements SupportReplyNotifier {

	private static final String TITLE = "문의하신 내용에 답변이 왔어요";
	private static final String DEEP_LINK = "meongcoach://support-chat";

	private final EventNotificationDispatcher eventDispatcher;

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public void notifyReply(ChannelTalkWebhookRequest request) {
		SupportReplyCommand command = request.toCommand();
		Optional<SupportReply> result = SupportReply.from(command);
		if (result.isEmpty()) {
			return;
		}

		SupportReply reply = result.get();
		Long userId = reply.recipientUserId();
		String body = reply.notificationBody();
		EventNotificationDispatchRequest dispatch = new EventNotificationDispatchRequest("SUPPORT_REPLY", "USERS",
				List.of(userId), TITLE, body, DEEP_LINK);
		eventDispatcher.dispatch(dispatch);
	}
}
