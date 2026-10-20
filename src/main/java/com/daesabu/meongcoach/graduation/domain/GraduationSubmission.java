package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "graduation_submissions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_graduation_submissions_graduation_id_attempt_no",
				columnNames = {"graduation_id", "attempt_no"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GraduationSubmission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "graduation_id", nullable = false)
	private Long graduationId;

	@Column(name = "attempt_no", nullable = false)
	private int attemptNo;

	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	@Column(nullable = false)
	private Instant submittedAt;

	public static GraduationSubmission record(TopicGraduation graduation) {
		GraduationSubmission submission = new GraduationSubmission();
		submission.graduationId = requireNonNull(graduation.getId());
		submission.attemptNo = graduation.getAttemptNo();
		submission.videoObjectKey = requireNonNull(graduation.getVideoObjectKey());
		submission.submittedAt = requireNonNull(graduation.getSubmittedAt());
		return submission;
	}

	public void replaceVideo(TopicGraduation graduation) {
		state(attemptNo == graduation.getAttemptNo(), "같은 차수의 기록만 바꿀 수 있다");
		this.videoObjectKey = requireNonNull(graduation.getVideoObjectKey());
		this.submittedAt = requireNonNull(graduation.getSubmittedAt());
	}
}
