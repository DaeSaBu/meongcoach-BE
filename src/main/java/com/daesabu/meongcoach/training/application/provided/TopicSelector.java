package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import jakarta.validation.Valid;

public interface TopicSelector {

	/**
	 * 강아지가 마지막으로 진입한 토픽을 기록한다. 토픽이 없으면 {@code TopicNotFoundException}을 던진다.
	 * 강아지 ID가 없으면 사용자가 선택한 강아지에 기록한다.
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	void selectTopic(Long userId, Long dogId, @Valid TopicSelectionRequest topicSelectionRequest);
}
