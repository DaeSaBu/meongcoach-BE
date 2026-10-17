package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationQueryService implements GraduationFinder {

	private final TopicGraduationRepository topicGraduationRepository;
	private final TopicFinder topicFinder;
	private final TopicCompletionChecker topicCompletionChecker;
	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;

	@Override
	public GraduationDetailResult findGraduation(Long userId, Long topicId) {
		TopicResult topic = topicFinder.findTopic(topicId);
		boolean eligible = topicCompletionChecker.isCompleted(userId, topicId);
		Optional<TopicGraduation> graduation = topicGraduationRepository.findByUserIdAndTopicId(userId, topicId);
		if (graduation.isEmpty()) {
			return GraduationDetailResult.notSubmitted(topic, eligible);
		}
		String videoUrl = videoDownloadUrlIssuer.issue(graduation.get().getVideoObjectKey()).downloadUrl();
		return new GraduationDetailResult(topic, eligible, graduation.get(), videoUrl);
	}
}
