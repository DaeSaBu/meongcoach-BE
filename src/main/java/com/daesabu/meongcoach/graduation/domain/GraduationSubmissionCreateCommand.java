package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;

public record GraduationSubmissionCreateCommand(int attemptNo, String videoObjectKey, Instant submittedAt) {
}
