package com.daesabu.meongcoach.training.adapter.webapi.dto;

/**
 * 커리큘럼 화면 변경 응답. 선택된 토픽 ID를 담는다.
 */
public record TopicSelectResponse(Long topicId) {

	public static TopicSelectResponse from(Long topicId) {
		return new TopicSelectResponse(topicId);
	}
}
