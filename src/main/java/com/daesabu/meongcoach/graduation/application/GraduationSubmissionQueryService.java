package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmissionFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.AdminSubmissionResult;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationSubmissionQueryService implements GraduationSubmissionFinder {

	private final TopicGraduationRepository topicGraduationRepository;
	private final GraduationSubmissionRepository graduationSubmissionRepository;
	private final TopicFinder topicFinder;
	private final VideoDownloadUrlIssuer videoDownloadUrlIssuer;

	@Override
	public List<AdminSubmissionResult> findSubmissions(String result) {
		SubmissionResult filter = SubmissionResult.from(result);
		List<GraduationSubmission> submissions = graduationSubmissionRepository.findAllByResultOrderBySubmittedAtAscIdAsc(filter);
		List<Long> graduationIds = submissions.stream().map(GraduationSubmission::getGraduationId).distinct().toList();
		Map<Long, TopicGraduation> graduations = topicGraduationRepository.findAllById(graduationIds).stream()
				.collect(Collectors.toMap(TopicGraduation::getId, Function.identity()));
		Map<Long, String> topicTitles = topicFinder.findAllOrdered().stream()
				.collect(Collectors.toMap(TopicResult::id, TopicResult::title));
		return submissions.stream()
				.map(submission -> toResult(submission, graduations.get(submission.getGraduationId()), topicTitles))
				.toList();
	}

	private AdminSubmissionResult toResult(GraduationSubmission submission, TopicGraduation graduation,
	                                       Map<Long, String> topicTitles) {
		String videoUrl = videoDownloadUrlIssuer.issue(submission.getVideoObjectKey()).downloadUrl();
		String topicTitle = topicTitles.getOrDefault(graduation.getTopicId(), "");
		return new AdminSubmissionResult(submission, graduation, topicTitle, videoUrl);
	}
}
