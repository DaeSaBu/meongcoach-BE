package com.daesabu.meongcoach.graduation.domain;

public record GraduationSubmittedEvent(Long submissionId, Long userId, Long topicId) {

	public static GraduationSubmittedEvent from(GraduationSubmission submission) {
		return new GraduationSubmittedEvent(submission.getId(), submission.getUserId(), submission.getTopicId());
	}
}
