package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import jakarta.validation.Valid;

public interface TopicSelector {

	void selectTopic(Long userId, @Valid TopicSelectionRequest topicSelectionRequest);
}
