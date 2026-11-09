package com.daesabu.meongcoach.support.adapter.integration;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.support.domain.ChatReplyCreated;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatReplyPushListener {
	private final EventNotificationDispatcher dispatcher;

	@Async
	@TransactionalEventListener
	public void notifyReply(ChatReplyCreated event) {
		try {
			var request =
					new EventNotificationDispatchRequest(
							"SUPPORT_REPLY",
							"USERS",
							List.of(event.userId()),
							"문의하신 내용에 답변이 왔어요",
							"문의 화면에서 답변을 확인해 주세요.",
							"meongcoach://support-chat");
			dispatcher.dispatch(request);
		} catch (RuntimeException exception) {
			log.warn("상담 답변 푸시 발송 실패: messageId={}", event.messageId());
		}
	}
}
