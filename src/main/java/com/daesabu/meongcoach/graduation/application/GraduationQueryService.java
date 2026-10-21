package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;

import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
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

	private final GraduationSubmissionRepository graduationSubmissionRepository;
	private final TopicFinder topicFinder;
	private final TopicCompletionChecker topicCompletionChecker;
	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;

	@Override
	public GraduationDetailResult findGraduation(Long userId, Long topicId) {
		TopicResult topic = topicFinder.findTopic(topicId);
		boolean eligible = topicCompletionChecker.isCompleted(userId, topicId);
		Optional<GraduationSubmission> graduation = graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(
				userId, topicId);
		if (graduation.isEmpty()) {
			return GraduationDetailResult.notSubmitted(topic, eligible);
		}
		String videoUrl = videoDownloadUrlIssuer.issue(graduation.get().getVideoObjectKey()).downloadUrl();
		return new GraduationDetailResult(topic, eligible, graduation.get(), videoUrl);
	}

	@Override
	public List<GraduationSummaryResult> findGraduations(Long userId) {
		List<GraduationSubmission> submissions = graduationSubmissionRepository.findLatestByUserId(userId);
		if (submissions.isEmpty()) {
			return List.of();
		}
		List<Long> topicIds = submissions.stream().map(GraduationSubmission::getTopicId).toList();
		List<TopicResult> topics = topicFinder.findTopics(topicIds);
		return submissions.stream().map(submission -> new GraduationSummaryResult(submission,
				findTopic(topics, submission.getTopicId()))).toList();
	}

	private TopicResult findTopic(List<TopicResult> topics, Long topicId) {
		return topics.stream()
				.filter(topic -> topic.id().equals(topicId))
				.findFirst()
				.orElseThrow();
	}
}
