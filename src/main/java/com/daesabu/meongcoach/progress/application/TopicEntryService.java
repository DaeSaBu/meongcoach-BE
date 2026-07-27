package com.daesabu.meongcoach.progress.application;

import com.daesabu.meongcoach.progress.application.provided.TopicEntryFinder;
import com.daesabu.meongcoach.progress.application.provided.TopicEntryRecorder;
import com.daesabu.meongcoach.progress.application.required.UserCurriculumProgressRepository;
import com.daesabu.meongcoach.progress.domain.UserCurriculumProgress;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토픽 진입 조회·기록 서비스. 모듈 공개 API 두 가지를 함께 구현한다.
 */
@Service
@RequiredArgsConstructor
public class TopicEntryService implements TopicEntryFinder, TopicEntryRecorder {

	private final UserCurriculumProgressRepository userCurriculumProgressRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> findLatestEnteredTopicId(Long userId) {
		return userCurriculumProgressRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId)
				.map(UserCurriculumProgress::getTopicId);
	}

	@Override
	@Transactional
	public void enterTopic(Long userId, Long topicId) {
		userCurriculumProgressRepository.findByUserIdAndTopicId(userId, topicId)
				.ifPresentOrElse(
						UserCurriculumProgress::reenter,
						() -> userCurriculumProgressRepository.save(UserCurriculumProgress.enter(userId, topicId)));
	}
}
