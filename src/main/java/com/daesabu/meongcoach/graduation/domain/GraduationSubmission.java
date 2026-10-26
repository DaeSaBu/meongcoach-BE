package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationReviewPendingException;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "graduation_submissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GraduationSubmission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;
	@Column(nullable = false)
	private Long userId;
	@Column(nullable = false)
	private Long topicId;

	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	@Column(nullable = false)
	private Instant submittedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GraduationSubmissionStatus status;

	private Instant reviewedAt;


	@Column(length = 512)
	private String retakeImageUrl;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private GraduationReview review = new GraduationReview(null, List.of());

	public static GraduationSubmission create(GraduationSubmissionCommand command) {
		GraduationSubmission submission = new GraduationSubmission();
		submission.userId = requireNonNull(command.userId());
		submission.topicId = requireNonNull(command.topicId());
		submission.videoObjectKey = requireNonNull(command.videoObjectKey());
		submission.submittedAt = requireNonNull(command.submittedAt());
		submission.status = GraduationSubmissionStatus.SUBMITTED;
		return submission;
	}

	public void validateCanSubmit() {
		if (status == GraduationSubmissionStatus.APPROVED) {
			throw new GraduationAlreadyApprovedException(topicId);
		}
		if (status == GraduationSubmissionStatus.SUBMITTED) {
			throw new GraduationReviewPendingException();
		}
	}

	public void approve(String note, Instant now) {
		state(status == GraduationSubmissionStatus.SUBMITTED, "제출 상태에서만 판정할 수 있다");
		Instant reviewedAt = requireNonNull(now);
		this.review = new GraduationReview(normalize(note), List.of());
		this.reviewedAt = reviewedAt;
		this.status = GraduationSubmissionStatus.APPROVED;
	}

	public void requestRevision(GraduationRevisionCommand command) {
		state(status == GraduationSubmissionStatus.SUBMITTED, "제출 상태에서만 판정할 수 있다");
		String normalized = normalize(command.note());
		isTrue(normalized != null, "보완 요청에는 메모가 필요하다");
		Instant reviewedAt = requireNonNull(command.reviewedAt());
		this.review = new GraduationReview(normalized, command.retakeInstructions());
		this.reviewedAt = reviewedAt;
		this.retakeImageUrl = command.retakeImageUrl();
		this.status = GraduationSubmissionStatus.REVISION_REQUESTED;
	}

	private static String normalize(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		return note.strip();
	}
}
