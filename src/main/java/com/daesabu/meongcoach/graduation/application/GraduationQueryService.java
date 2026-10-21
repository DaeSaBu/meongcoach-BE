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
		GraduationSubmission submission = graduation.get();
		if (submission.getVideoObjectKey() == null) {
			return new GraduationDetailResult(topic, eligible, submission, null);
		}
		String videoUrl = videoDownloadUrlIssuer.issue(submission.getVideoObjectKey()).downloadUrl();
		return new GraduationDetailResult(topic, eligible, submission, videoUrl);
	}

	@Override
	public List<GraduationSummaryResult> findGraduations(Long userId) {
		List<GraduationSubmission> submissions = graduationSubmissionRepository.findLatestByUserId(userId);
		List<Long> topicIds = submissions.stream().map(GraduationSubmission::getTopicId).toList();
		List<TopicResult> topics = topicFinder.findTopics(topicIds);
		return submissions.stream()
				.map(submission -> summarize(submission, topics)).toList();
	}

	private GraduationSummaryResult summarize(GraduationSubmission submission, List<TopicResult> topics) {
		TopicResult topic = findTopic(topics, submission.getTopicId());
		return new GraduationSummaryResult(submission, topic);
	}

	private TopicResult findTopic(List<TopicResult> topics, Long topicId) {
		return topics.stream()
				.filter(topic -> topic.id().equals(topicId))
				.findFirst()
				.orElseThrow();
	}
}
