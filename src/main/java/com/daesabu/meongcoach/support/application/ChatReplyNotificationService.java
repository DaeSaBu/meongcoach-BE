package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.support.domain.ChatReplySentEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatReplyNotificationService {
    private final EventNotificationDispatcher notifications;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notifyReply(ChatReplySentEvent event) {
        var request = new EventNotificationDispatchRequest(
                "SUPPORT_REPLY", "USERS", List.of(event.userId()),
                "문의하신 내용에 답변이 왔어요", "문의 화면에서 답변을 확인해 주세요.",
                "meongcoach://support-chat");
        try {
            notifications.dispatch(request);
        } catch (RuntimeException exception) {
            log.warn("상담 푸시 발송 실패: messageId={}, error={}",
                    event.messageId(), exception.getClass().getSimpleName());
        }
    }
}
