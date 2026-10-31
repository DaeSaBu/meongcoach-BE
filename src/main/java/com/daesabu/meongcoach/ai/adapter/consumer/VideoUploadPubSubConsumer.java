package com.daesabu.meongcoach.ai.adapter.consumer;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.shared.exception.DomainException;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadPubSubConsumer implements MessageReceiver {

	static final String EVENT_TYPE_ATTRIBUTE = "eventType";
	static final String OBJECT_ID_ATTRIBUTE = "objectId";
	static final String OBJECT_FINALIZE = "OBJECT_FINALIZE";

	private final AiReportGenerator aiReportGenerator;

	@Override
	public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
		try {
			dispatch(message);
		} catch (Exception e) {
			log.error("Pub/Sub 메시지 처리에 실패했지만 무한 재전달을 막기 위해 메시지를 버린다", e);
		} finally {
			consumer.ack();
		}
	}

	private void dispatch(PubsubMessage message) {
		String eventType = message.getAttributesOrDefault(EVENT_TYPE_ATTRIBUTE, "");
		if (!OBJECT_FINALIZE.equals(eventType)) {
			log.info("업로드 완료가 아닌 GCS 이벤트를 무시한다: {}", eventType);
			return;
		}
		String objectKey = message.getAttributesOrDefault(OBJECT_ID_ATTRIBUTE, "");
		if (objectKey.isBlank()) {
			log.warn("객체 키가 없는 GCS 이벤트를 버린다");
			return;
		}
		try {
			aiReportGenerator.generate(objectKey);
		} catch (DomainException e) {
			log.warn("처리할 수 없는 GCS 객체 키라 리포트 생성을 건너뛴다: {}", objectKey, e);
		}
	}
}
