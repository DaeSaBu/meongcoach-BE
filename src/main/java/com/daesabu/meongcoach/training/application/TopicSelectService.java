package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.TopicEntryRecorder;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicSelectService implements TopicSelector {

	private final TopicRepository topicRepository;
	private final TopicEntryRecorder topicEntryRecorder;

	@Override
	@Transactional
	public void selectTopic(Long userId, TopicSelectionRequest topicSelectionRequest) {
		validateExistingTopic(topicSelectionRequest);

		topicEntryRecorder.enterTopic(userId, topicSelectionRequest.topicId());
	}

	private void validateExistingTopic(TopicSelectionRequest topicSelectionRequest) {
		if (!topicRepository.existsById(topicSelectionRequest.topicId())) {
			throw new TopicNotFoundException(topicSelectionRequest.topicId());
		}
	}
}
