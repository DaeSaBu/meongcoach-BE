package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationVideo;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.media.application.provided.MediaDownloadUrlIssuer;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.List;
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
	private final MediaDownloadUrlIssuer mediaDownloadUrlIssuer;

	@Override
	public GraduationDetailResult findGraduation(Long userId, Long topicId) {
		TopicResult topic = topicFinder.findTopic(topicId);
		boolean eligible = topicCompletionChecker.isCompleted(userId, topicId);
		Optional<TopicGraduation> graduation = topicGraduationRepository.findByUserIdAndTopicId(userId, topicId);
		if (graduation.isEmpty()) {
			return GraduationDetailResult.notSubmitted(topic, eligible);
		}
		String videoUrl = mediaDownloadUrlIssuer.issue(GraduationVideo.SCOPE, graduation.get().getVideoObjectKey());
		return new GraduationDetailResult(topic, eligible, graduation.get(), videoUrl);
	}

	@Override
	public List<GraduationSummaryResult> findGraduations(Long userId) {
		List<TopicGraduation> graduations = topicGraduationRepository.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId);
		if (graduations.isEmpty()) {
			return List.of();
		}
		List<Long> topicIds = graduations.stream().map(TopicGraduation::getTopicId).toList();
		List<TopicResult> topics = topicFinder.findTopics(topicIds);
		return graduations.stream()
				.map(graduation -> new GraduationSummaryResult(graduation, findTopic(topics, graduation.getTopicId())))
				.toList();
	}

	private TopicResult findTopic(List<TopicResult> topics, Long topicId) {
		return topics.stream()
				.filter(topic -> topic.id().equals(topicId))
				.findFirst()
				.orElseThrow();
	}
}
