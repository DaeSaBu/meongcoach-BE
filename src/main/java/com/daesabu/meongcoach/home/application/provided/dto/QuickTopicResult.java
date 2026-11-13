package com.daesabu.meongcoach.home.application.provided.dto;

import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;

public record QuickTopicResult(TopicProgressResult topic, QuickTopicRole role) {

	public static QuickTopicResult from(TopicProgressResult topic) {
		if (topic.status() == ProgressStatus.IN_PROGRESS) {
			return new QuickTopicResult(topic, QuickTopicRole.IN_PROGRESS);
		}
		return new QuickTopicResult(topic, QuickTopicRole.NOT_STARTED);
	}
}
