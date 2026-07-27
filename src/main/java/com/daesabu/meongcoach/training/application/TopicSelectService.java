package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.TopicEntryRecorder;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커리큘럼 화면 변경 서비스. 토픽 존재를 확인한 뒤 진입 기록은 progress 모듈의 공개 API에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class TopicSelectService implements TopicSelector {

	private final TopicRepository topicRepository;

	private final TopicEntryRecorder topicEntryRecorder;

	@Override
	@Transactional
	public void selectTopic(Long userId, Long topicId) {
		if (!topicRepository.existsById(topicId)) {
			throw new TopicNotFoundException(topicId);
		}

		topicEntryRecorder.enterTopic(userId, topicId);
	}
}
