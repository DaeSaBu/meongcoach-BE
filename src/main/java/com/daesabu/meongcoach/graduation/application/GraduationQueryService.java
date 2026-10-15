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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationQueryService implements GraduationFinder {

	private static final Comparator<GraduationSummaryResult> LATEST_SUBMISSION_FIRST = Comparator
			.comparing((GraduationSummaryResult result) -> result.latestSubmission().getSubmittedAt())
			.thenComparing(result -> result.latestSubmission().getId())
			.reversed();

	private final TopicGraduationRepository topicGraduationRepository;
	private final GraduationSubmissionRepository graduationSubmissionRepository;
	private final TopicFinder topicFinder;
	private final TopicCompletionChecker topicCompletionChecker;
	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;

	@Override
	public boolean isApproved(Long userId, Long topicId) {
		return topicGraduationRepository.findByUserIdAndTopicId(userId, topicId)
				.filter(TopicGraduation::isApproved)
				.isPresent();
	}

	@Override
	public List<Long> findApprovedTopicIds(Long userId) {
		return topicGraduationRepository.findApprovedTopicIds(userId);
	}

	@Override
	public GraduationDetailResult findGraduation(Long userId, Long topicId) {
		TopicResult topic = topicFinder.findTopic(topicId);
		boolean eligible = topicCompletionChecker.isCompleted(userId, topicId);
		Optional<TopicGraduation> graduation = topicGraduationRepository.findByUserIdAndTopicId(userId, topicId);
		if (graduation.isEmpty()) {
			return GraduationDetailResult.notSubmitted(topic, eligible);
		}
		GraduationSubmission latest = graduationSubmissionRepository
				.findTopByGraduationIdOrderByAttemptNoDesc(graduation.get().getId())
				.orElseThrow();
		String videoUrl = videoDownloadUrlIssuer.issue(latest.getVideoObjectKey()).downloadUrl();
		return new GraduationDetailResult(topic, eligible, graduation.get(), latest, videoUrl);
	}

	@Override
	public List<GraduationSummaryResult> findGraduations(Long userId) {
		List<TopicGraduation> graduations = topicGraduationRepository.findAllByUserId(userId);
		List<Long> graduationIds = graduations.stream().map(TopicGraduation::getId).toList();
		Map<Long, GraduationSubmission> latestByGraduationId = graduationSubmissionRepository
				.findAllByGraduationIdIn(graduationIds).stream()
				.collect(Collectors.toMap(GraduationSubmission::getGraduationId, Function.identity(),
						BinaryOperator.maxBy(Comparator.comparingInt(GraduationSubmission::getAttemptNo))));
		Map<Long, String> topicTitles = topicFinder.findAllOrdered().stream()
				.collect(Collectors.toMap(TopicResult::id, TopicResult::title));
		return graduations.stream()
				.map(graduation -> new GraduationSummaryResult(graduation,
						topicTitles.getOrDefault(graduation.getTopicId(), ""), latestByGraduationId.get(graduation.getId())))
				.sorted(LATEST_SUBMISSION_FIRST)
				.toList();
	}
}
