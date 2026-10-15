package com.daesabu.meongcoach.graduation.application.provided.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

public record AdminSubmissionResult(GraduationSubmission submission, TopicGraduation graduation, String topicTitle,
                                    String videoUrl) {
}
