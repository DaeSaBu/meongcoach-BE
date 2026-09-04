package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.Topic;

/**
 * 다른 모듈에 공개하는 토픽 요약 정보.
 */
public record TopicSummary(Long id, String title, String description) {

	public static TopicSummary from(Topic topic) {
		return new TopicSummary(topic.getId(), topic.getTitle(), topic.getDescription());
	}
}
