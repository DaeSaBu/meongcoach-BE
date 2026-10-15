package com.daesabu.meongcoach.ai.adapter.consumer;

import com.daesabu.meongcoach.ai.adapter.consumer.dto.S3EventMessage;
import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.shared.exception.DomainException;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadSqsConsumer {

	private static final String OBJECT_CREATED_PREFIX = "ObjectCreated";
	private static final String TRAINING_VIDEO_KEY_PREFIX = "videos/training/";

	private final AiReportGenerator aiReportGenerator;
	private final ObjectMapper objectMapper;

	@SqsListener("${meongcoach.ai.video-queue}")
	public void consume(String message) {
		try {
			dispatch(message);
		} catch (Exception e) {
			log.error("SQS 메시지 처리에 실패했지만 무한 재전달을 막기 위해 메시지를 버린다", e);
		}
	}

	private void dispatch(String message) {
		S3EventMessage event;
		try {
			event = objectMapper.readValue(message, S3EventMessage.class);
		} catch (JacksonException e) {
			log.warn("S3 이벤트 형식이 아닌 SQS 메시지를 버린다", e);
			return;
		}
		if (event.records() == null || event.records().isEmpty()) {
			log.info("Records 없는 SQS 메시지를 무시한다");
			return;
		}
		for (S3EventMessage.EventRecord record : event.records()) {
			handle(record);
		}
	}

	private void handle(S3EventMessage.EventRecord record) {
		if (record.eventName() == null || !record.eventName().startsWith(OBJECT_CREATED_PREFIX)) {
			log.info("업로드 완료가 아닌 S3 이벤트를 무시한다: {}", record.eventName());
			return;
		}
		String objectKey = objectKeyOf(record);
		if (objectKey == null) {
			return;
		}
		if (!objectKey.startsWith(TRAINING_VIDEO_KEY_PREFIX)) {
			log.debug("훈련 영상이 아닌 객체 키라 리포트 생성을 건너뛴다: {}", objectKey);
			return;
		}
		try {
			aiReportGenerator.generate(objectKey);
		} catch (DomainException e) {
			log.warn("처리할 수 없는 S3 객체 키라 리포트 생성을 건너뛴다: {}", objectKey, e);
		} catch (Exception e) {
			log.error("리포트 생성에 실패했지만 SQS 무한 재전달을 막기 위해 메시지를 버린다: {}", objectKey, e);
		}
	}

	private String objectKeyOf(S3EventMessage.EventRecord record) {
		if (record.s3() == null || record.s3().object() == null || record.s3().object().key() == null) {
			log.warn("객체 키가 없는 S3 이벤트를 버린다: {}", record.eventName());
			return null;
		}
		String rawKey = record.s3().object().key();
		try {
			return URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			log.warn("URL 디코딩할 수 없는 객체 키를 버린다: {}", rawKey, e);
			return null;
		}
	}
}
