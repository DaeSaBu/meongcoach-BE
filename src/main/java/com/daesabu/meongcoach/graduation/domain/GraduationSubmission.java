package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.state;

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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GraduationSubmissionStatus status;

	private Instant reviewedAt;

	@Column(length = 500)
	private String reviewNote;

	@Column(length = 512)
	private String retakeImageUrl;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private List<String> retakeInstructions = new ArrayList<>();

	public static GraduationSubmission from(TopicGraduation graduation) {
		GraduationSubmission submission = new GraduationSubmission();
		submission.status = GraduationSubmissionStatus.SUBMITTED;
		submission.graduationId = requireNonNull(graduation.getId());
		submission.attemptNo = graduation.getAttemptNo();
		submission.videoObjectKey = requireNonNull(graduation.getVideoObjectKey());
		submission.submittedAt = requireNonNull(graduation.getSubmittedAt());
		return submission;
	}

	public void replaceVideo(TopicGraduation graduation) {
		state(status == GraduationSubmissionStatus.SUBMITTED, "판정 전 제출의 영상만 바꿀 수 있다");
		state(graduationId.equals(graduation.getId()), "같은 수료의 기록만 바꿀 수 있다");
		state(attemptNo == graduation.getAttemptNo(), "같은 차수의 기록만 바꿀 수 있다");
		this.videoObjectKey = requireNonNull(graduation.getVideoObjectKey());
		this.submittedAt = requireNonNull(graduation.getSubmittedAt());
	}

	public List<String> getRetakeInstructions() {
		return List.copyOf(retakeInstructions);
	}

	void approve(String note, Instant now) {
		state(status == GraduationSubmissionStatus.SUBMITTED, "제출 상태에서만 판정할 수 있다");
		Instant reviewedAt = requireNonNull(now);
		this.reviewNote = normalize(note);
		this.reviewedAt = reviewedAt;
		this.status = GraduationSubmissionStatus.APPROVED;
	}

	void requestRevision(GraduationRevisionCommand command) {
		state(status == GraduationSubmissionStatus.SUBMITTED, "제출 상태에서만 판정할 수 있다");
		String normalized = normalize(command.note());
		isTrue(normalized != null, "보완 요청에는 메모가 필요하다");
		Instant reviewedAt = requireNonNull(command.reviewedAt());
		this.reviewNote = normalized;
		this.reviewedAt = reviewedAt;
		this.retakeInstructions = new ArrayList<>(command.retakeInstructions());
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
