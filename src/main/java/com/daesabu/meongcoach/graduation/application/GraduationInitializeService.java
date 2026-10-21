package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.training.application.provided.TopicCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationInitializeService {
	private final GraduationSubmissionRepository graduationSubmissionRepository;

	@EventListener
	@Transactional
	public void initialize(TopicCompletedEvent event) {
		if (graduationSubmissionRepository.existsByUserIdAndTopicId(event.userId(), event.topicId())) {
			return;
		}
		GraduationSubmission submission = GraduationSubmission.notSubmitted(event.userId(), event.topicId());
		graduationSubmissionRepository.save(submission);
	}
}
