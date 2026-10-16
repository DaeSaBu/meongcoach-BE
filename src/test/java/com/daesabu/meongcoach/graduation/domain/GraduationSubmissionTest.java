package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GraduationSubmissionTest {

	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-15T05:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/clip.mp4";

	@Test
	void 수료_행의_현재_제출을_그대로_기록한다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L, VIDEO_KEY, SUBMITTED_AT);
		ReflectionTestUtils.setField(graduation, "id", 1L);

		GraduationSubmission submission = GraduationSubmission.record(graduation);

		assertThat(submission.getGraduationId()).isEqualTo(1L);
		assertThat(submission.getAttemptNo()).isEqualTo(1);
		assertThat(submission.getVideoObjectKey()).isEqualTo(VIDEO_KEY);
		assertThat(submission.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
	}
}
