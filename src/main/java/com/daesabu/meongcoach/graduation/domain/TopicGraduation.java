package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "topic_graduations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_topic_graduations_user_id_topic_id", columnNames = {"user_id", "topic_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicGraduation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "topic_id", nullable = false)
	private Long topicId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GraduationStatus status;

	@Column(name = "attempt_no", nullable = false)
	private int attemptNo;

	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	@Column(nullable = false)
	private Instant submittedAt;

	private Instant approvedAt;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "current_submission_id", unique = true)
	private GraduationSubmission currentSubmission;

	public static TopicGraduation start(Long userId, Long topicId, String videoObjectKey, Instant now) {
		TopicGraduation graduation = new TopicGraduation();
		graduation.userId = requireNonNull(userId);
		graduation.topicId = requireNonNull(topicId);
		graduation.status = GraduationStatus.SUBMITTED;
		graduation.attemptNo = 1;
		graduation.videoObjectKey = requireNonNull(videoObjectKey);
		graduation.submittedAt = requireNonNull(now);
		return graduation;
	}

	public void resubmit(String videoObjectKey, Instant now) {
		state(!isApproved(), "승인된 수료에는 다시 제출할 수 없다");
		if (status == GraduationStatus.REVISION_REQUESTED) {
			this.attemptNo++;
			this.currentSubmission = null;
		}
		this.status = GraduationStatus.SUBMITTED;
		this.videoObjectKey = requireNonNull(videoObjectKey);
		this.submittedAt = requireNonNull(now);
	}

	public void useSubmission(GraduationSubmission submission) {
		requireNonNull(submission);
		state(id.equals(submission.getGraduationId()), "같은 수료의 기록만 연결할 수 있다");
		state(attemptNo == submission.getAttemptNo(), "현재 차수의 기록만 연결할 수 있다");
		this.currentSubmission = submission;
	}

	public void approve(String note, Instant now) {
		state(isSubmitted(), "제출 상태의 수료만 승인할 수 있다");
		requireNonNull(currentSubmission).approve(note, now);
		this.status = GraduationStatus.APPROVED;
		this.approvedAt = now;
	}

	public void requestRevision(String note, Instant now) {
		requestRevision(new GraduationRevisionCommand(note, List.of(), null, now));
	}

	public void requestRevision(GraduationRevisionCommand command) {
		state(isSubmitted(), "제출 상태의 수료만 보완 요청할 수 있다");
		requireNonNull(currentSubmission).requestRevision(command);
		this.status = GraduationStatus.REVISION_REQUESTED;
	}

	public Instant getReviewedAt() {
		if (currentSubmission == null) {
			return null;
		}
		return currentSubmission.getReviewedAt();
	}

	public String getReviewNote() {
		if (currentSubmission == null) {
			return null;
		}
		return currentSubmission.getReviewNote();
	}

	public List<String> getRetakeInstructions() {
		if (currentSubmission == null) {
			return List.of();
		}
		return currentSubmission.getRetakeInstructions();
	}

	public String getRetakeFramingIconCode() {
		if (currentSubmission == null) {
			return null;
		}
		return currentSubmission.getRetakeFramingIconCode();
	}

	public boolean isApproved() {
		return status == GraduationStatus.APPROVED;
	}

	public boolean isSubmitted() {
		return status == GraduationStatus.SUBMITTED;
	}
}
