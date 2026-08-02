package com.daesabu.meongcoach.ai.adapter.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * S3 이벤트 알림 표준 포맷 중 소비에 필요한 부분만 담는다.
 * s3:TestEvent처럼 Records가 없는 메시지도 수신되므로 records는 null일 수 있다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record S3EventMessage(@JsonProperty("Records") List<EventRecord> records) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EventRecord(String eventName, S3 s3) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record S3(S3Object object) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record S3Object(String key) {
	}
}
