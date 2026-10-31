package com.daesabu.meongcoach.ai.adapter.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.media.domain.exception.InvalidMediaObjectKeyException;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.pubsub.v1.PubsubMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VideoUploadPubSubConsumerTest {

	private RecordingAiReportGenerator aiReportGenerator;
	private RecordingAckReplyConsumer ackReplyConsumer;
	private VideoUploadPubSubConsumer consumer;

	@BeforeEach
	void setUp() {
		aiReportGenerator = new RecordingAiReportGenerator();
		ackReplyConsumer = new RecordingAckReplyConsumer();
		consumer = new VideoUploadPubSubConsumer(aiReportGenerator);
	}

	private static PubsubMessage gcsEvent(String eventType, String objectId) {
		PubsubMessage.Builder builder = PubsubMessage.newBuilder()
				.putAttributes("bucketId", "test-ai-video-bucket")
				.putAttributes("objectGeneration", "1700000000000000")
				.putAttributes("payloadFormat", "JSON_API_V1")
				.putAttributes("eventTime", "2026-10-26T07:51:20.063Z");
		if (eventType != null) {
			builder.putAttributes("eventType", eventType);
		}
		if (objectId != null) {
			builder.putAttributes("objectId", objectId);
		}
		return builder.build();
	}

	@Test
	void 업로드_완료_이벤트의_객체_키로_리포트_생성을_위임한다() {
		consumer.receiveMessage(gcsEvent("OBJECT_FINALIZE", "videos/training/7/key.mp4"), ackReplyConsumer);

		assertThat(aiReportGenerator.objectKeys).containsExactly("videos/training/7/key.mp4");
		assertThat(ackReplyConsumer.acked).isTrue();
	}

	@Test
	void 객체_키는_인코딩_없이_attributes의_값을_그대로_위임한다() {
		consumer.receiveMessage(gcsEvent("OBJECT_FINALIZE", "videos/training/7/my video=1.mp4"), ackReplyConsumer);

		assertThat(aiReportGenerator.objectKeys).containsExactly("videos/training/7/my video=1.mp4");
	}

	@Test
	void 업로드_완료가_아닌_이벤트는_무시하고_ack한다() {
		consumer.receiveMessage(gcsEvent("OBJECT_DELETE", "videos/training/7/key.mp4"), ackReplyConsumer);

		assertThat(aiReportGenerator.objectKeys).isEmpty();
		assertThat(ackReplyConsumer.acked).isTrue();
	}

	@Test
	void eventType이_없는_메시지는_무시하고_ack한다() {
		consumer.receiveMessage(gcsEvent(null, "videos/training/7/key.mp4"), ackReplyConsumer);

		assertThat(aiReportGenerator.objectKeys).isEmpty();
		assertThat(ackReplyConsumer.acked).isTrue();
	}

	@Test
	void 객체_키가_없는_이벤트는_버리고_ack한다() {
		consumer.receiveMessage(gcsEvent("OBJECT_FINALIZE", null), ackReplyConsumer);

		assertThat(aiReportGenerator.objectKeys).isEmpty();
		assertThat(ackReplyConsumer.acked).isTrue();
	}

	@Test
	void 도메인_검증에_실패한_키는_버리고_ack한다() {
		aiReportGenerator.failure = new InvalidMediaObjectKeyException("images/profile/7/key.png");

		assertThatCode(() -> consumer.receiveMessage(gcsEvent("OBJECT_FINALIZE", "images/profile/7/key.png"),
				ackReplyConsumer)).doesNotThrowAnyException();
		assertThat(ackReplyConsumer.acked).isTrue();
	}

	@Test
	void 리포트_생성이_실패해도_예외를_삼키고_ack한다() {
		aiReportGenerator.failure = new IllegalStateException("모델 호출 실패");

		assertThatCode(() -> consumer.receiveMessage(gcsEvent("OBJECT_FINALIZE", "videos/training/7/key.mp4"),
				ackReplyConsumer)).doesNotThrowAnyException();
		assertThat(ackReplyConsumer.acked).isTrue();
		assertThat(ackReplyConsumer.nacked).isFalse();
	}

	private static class RecordingAiReportGenerator implements AiReportGenerator {

		private final List<String> objectKeys = new ArrayList<>();
		private RuntimeException failure;

		@Override
		public void generate(String objectKey) {
			if (failure != null) {
				throw failure;
			}
			objectKeys.add(objectKey);
		}
	}

	private static class RecordingAckReplyConsumer implements AckReplyConsumer {

		private boolean acked;
		private boolean nacked;

		@Override
		public void ack() {
			acked = true;
		}

		@Override
		public void nack() {
			nacked = true;
		}
	}
}
