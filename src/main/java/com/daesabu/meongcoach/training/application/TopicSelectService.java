package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TopicSelectionRepository;
import com.daesabu.meongcoach.training.domain.TopicSelection;
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
	private final TopicSelectionRepository topicSelectionRepository;

	@Override
	@Transactional
	public void selectTopic(Long userId, TopicSelectionRequest topicSelectionRequest) {
		Long topicId = topicSelectionRequest.topicId();
		if (!topicRepository.existsById(topicId)) {
			throw new TopicNotFoundException(topicId);
		}
		topicSelectionRepository.findByUserId(userId)
				.ifPresentOrElse(selection -> selection.moveTo(topicId),
						() -> topicSelectionRepository.save(TopicSelection.enter(userId, topicId)));
	}
}
