package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.graduation.domain.exception.ReviewNoteRequiredException;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SubmissionResult result;

	@Column(nullable = false)
	private Instant submittedAt;

	private Instant reviewedAt;

	@Column(length = 500)
	private String reviewerNote;

	public static GraduationSubmission create(TopicGraduation graduation, GraduationSubmissionCreateCommand command) {
		GraduationSubmission submission = new GraduationSubmission();
		submission.graduationId = requireNonNull(graduation.getId());
		submission.attemptNo = command.attemptNo();
		submission.videoObjectKey = requireNonNull(command.videoObjectKey());
		submission.submittedAt = requireNonNull(command.submittedAt());
		submission.result = SubmissionResult.PENDING;
		return submission;
	}

	public void approve(String note, Instant now) {
		state(isPending(), "검수 대기 중인 제출만 판정할 수 있다");
		this.result = SubmissionResult.APPROVED;
		this.reviewedAt = requireNonNull(now);
		this.reviewerNote = normalize(note);
	}

	public void requestRevision(String note, Instant now) {
		state(isPending(), "검수 대기 중인 제출만 판정할 수 있다");
		String normalized = normalize(note);
		if (normalized == null) {
			throw new ReviewNoteRequiredException();
		}
		this.result = SubmissionResult.REVISION_REQUESTED;
		this.reviewedAt = requireNonNull(now);
		this.reviewerNote = normalized;
	}

	public void replace() {
		state(isPending(), "검수 대기 중인 제출만 교체할 수 있다");
		this.result = SubmissionResult.REPLACED;
	}

	public boolean isPending() {
		return result == SubmissionResult.PENDING;
	}

	private static String normalize(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		return note.strip();
	}
}
