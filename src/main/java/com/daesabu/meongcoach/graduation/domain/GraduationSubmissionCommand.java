package com.daesabu.meongcoach.graduation.domain;
import java.time.Instant;
public record GraduationSubmissionCommand(Long userId, Long topicId, String videoObjectKey, Instant submittedAt) {}
