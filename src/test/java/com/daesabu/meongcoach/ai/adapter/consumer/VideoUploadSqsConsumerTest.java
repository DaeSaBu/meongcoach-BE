package com.daesabu.meongcoach.ai.adapter.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("영상 업로드 SQS 컨슈머")
class VideoUploadSqsConsumerTest {

	private RecordingAiReportGenerator aiReportGenerator;
	private VideoUploadSqsConsumer consumer;

	@BeforeEach
	void setUp() {
		aiReportGenerator = new RecordingAiReportGenerator();
		consumer = new VideoUploadSqsConsumer(aiReportGenerator, JsonMapper.builder().build());
	}

	private static String s3Event(String eventName, String key) {
		return """
				{
					"Records": [
						{
							"eventVersion": "2.1",
							"eventSource": "aws:s3",
							"eventName": "%s",
							"s3": {
								"bucket": { "name": "test-video-bucket" },
								"object": { "key": "%s", "size": 1024 }
							}
						}
					]
				}
				""".formatted(eventName, key);
	}

	@Test
	@DisplayName("업로드 완료 이벤트의 객체 키로 리포트 생성을 위임한다")
	void consumeDelegatesObjectKeyToGenerator() throws Exception {
		consumer.consume(s3Event("ObjectCreated:Put", "videos/training/7/key.mp4"));

		assertThat(aiReportGenerator.objectKeys).containsExactly("videos/training/7/key.mp4");
	}

	@Test
	@DisplayName("실제 S3 이벤트 2.5 형식의 업로드 완료 메시지를 처리한다")
	void consumeSupportsActualS3EventVersion25() {
		consumer.consume("""
				{
					"Records": [
						{
							"eventVersion": "2.5",
							"eventSource": "aws:s3",
							"awsRegion": "ap-northeast-2",
							"eventTime": "2026-08-02T07:51:20.063Z",
							"eventName": "ObjectCreated:Put",
							"userIdentity": { "principalId": "AWS:test-uploader" },
							"requestParameters": { "sourceIPAddress": "127.0.0.1" },
							"responseElements": { "x-amz-request-id": "test-request-id" },
							"s3": {
								"s3SchemaVersion": "1.0",
								"configurationId": "meongcoach-dev-video-upload",
								"bucket": {
									"name": "meongcoach-dev-s3-files",
									"ownerIdentity": { "principalId": "test-owner" },
									"arn": "arn:aws:s3:::meongcoach-dev-s3-files"
								},
								"object": {
									"key": "videos/training/1/771b2834-6213-41ba-a3f0-dde9dcceefd0.mp4",
									"size": 7217500,
									"eTag": "test-etag",
									"sequencer": "006A6EF6F74389E232"
								}
							}
						}
					]
				}
				""");

		assertThat(aiReportGenerator.objectKeys)
				.containsExactly("videos/training/1/771b2834-6213-41ba-a3f0-dde9dcceefd0.mp4");
	}

	@Test
	@DisplayName("URL 인코딩된 객체 키는 디코딩해 위임한다")
	void consumeDecodesUrlEncodedKey() throws Exception {
		consumer.consume(s3Event("ObjectCreated:Put", "videos/training/7/my+video%3D1.mp4"));

		assertThat(aiReportGenerator.objectKeys).containsExactly("videos/training/7/my video=1.mp4");
	}

	@Test
	@DisplayName("여러 레코드가 오면 각각 위임한다")
	void consumeDelegatesEachRecord() throws Exception {
		consumer.consume("""
				{
					"Records": [
						{ "eventName": "ObjectCreated:Put", "s3": { "object": { "key": "videos/training/1/a.mp4" } } },
						{ "eventName": "ObjectCreated:Put", "s3": { "object": { "key": "videos/training/2/b.mp4" } } }
					]
				}
				""");

		assertThat(aiReportGenerator.objectKeys)
				.containsExactly("videos/training/1/a.mp4", "videos/training/2/b.mp4");
	}

	@Test
	@DisplayName("Records가 없는 테스트 이벤트는 무시한다")
	void consumeIgnoresMessageWithoutRecords() throws Exception {
		consumer.consume("""
				{ "Service": "Amazon S3", "Event": "s3:TestEvent", "Bucket": "test-video-bucket" }
				""");

		assertThat(aiReportGenerator.objectKeys).isEmpty();
	}

	@Test
	@DisplayName("업로드 완료가 아닌 이벤트는 무시한다")
	void consumeIgnoresNonObjectCreatedEvent() throws Exception {
		consumer.consume(s3Event("ObjectRemoved:Delete", "videos/training/7/key.mp4"));

		assertThat(aiReportGenerator.objectKeys).isEmpty();
	}

	@Test
	@DisplayName("S3 이벤트 형식이 아닌 메시지는 버리고 정상 반환한다")
	void consumeDropsUnparseableMessage() {
		consumer.consume("not-a-json");

		assertThat(aiReportGenerator.objectKeys).isEmpty();
	}

	@Test
	@DisplayName("도메인 검증에 실패한 키는 버리고 정상 반환한다")
	void consumeDropsKeyRejectedByDomainValidation() throws Exception {
		aiReportGenerator.failure = new InvalidVideoObjectKeyException("images/profile/7/key.png");

		consumer.consume(s3Event("ObjectCreated:Put", "images/profile/7/key.png"));
		// 예외가 나가지 않아야 SQS가 메시지를 삭제하고 무한 재시도를 하지 않는다
	}

	@Test
	@DisplayName("리포트 생성이 실패하면 예외를 전파해 재전달되게 한다")
	void consumePropagatesGenerationFailure() {
		aiReportGenerator.failure = new IllegalStateException("Gemini 호출 실패");

		assertThatThrownBy(() -> consumer.consume(s3Event("ObjectCreated:Put", "videos/training/7/key.mp4")))
				.isInstanceOf(IllegalStateException.class);
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
}
