package com.daesabu.meongcoach.graduation.application.required;

import java.time.Instant;

public record SubmissionNotice(Long graduationId, Long userId, Long topicId, String topicTitle, int attemptNo,
                               Instant submittedAt, String videoObjectKey, String videoUrl) {
}
