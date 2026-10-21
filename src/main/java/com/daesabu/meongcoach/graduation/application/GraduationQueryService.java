package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationQueryService implements GraduationFinder {

	private final TopicGraduationRepository topicGraduationRepository;
	private final GraduationSubmissionRepository graduationSubmissionRepository;
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
		GraduationSubmission submission = graduationSubmissionRepository
				.findByGraduationIdAndAttemptNo(graduation.get().getId(), graduation.get().getAttemptNo()).orElseThrow();
		return new GraduationDetailResult(topic, eligible, graduation.get(), submission, videoUrl);
	}

	@Override
	public List<GraduationSummaryResult> findGraduations(Long userId) {
		List<TopicGraduation> graduations = topicGraduationRepository.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId);
		if (graduations.isEmpty()) {
			return List.of();
		}
		List<Long> topicIds = graduations.stream().map(TopicGraduation::getTopicId).toList();
		List<TopicResult> topics = topicFinder.findTopics(topicIds);
		List<Long> graduationIds = graduations.stream().map(TopicGraduation::getId).toList();
		Map<Long, GraduationSubmission> submissions = graduationSubmissionRepository
				.findCurrentSubmissionsByGraduationIdIn(graduationIds).stream()
				.collect(Collectors.toMap(submission -> submission.getGraduation().getId(), Function.identity()));
		return graduations.stream()
				.map(graduation -> new GraduationSummaryResult(graduation,
						submissions.get(graduation.getId()), findTopic(topics, graduation.getTopicId())))
				.toList();
	}

	private TopicResult findTopic(List<TopicResult> topics, Long topicId) {
		return topics.stream()
				.filter(topic -> topic.id().equals(topicId))
				.findFirst()
				.orElseThrow();
	}
}
