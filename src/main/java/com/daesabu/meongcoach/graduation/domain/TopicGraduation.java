package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
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
		name = "topic_graduations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_topic_graduations_user_id_topic_id", columnNames = {"user_id", "topic_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicGraduation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	private Instant reviewedAt;

	@Column(length = 500)
	private String reviewNote;

	private Instant approvedAt;

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
		this.status = GraduationStatus.SUBMITTED;
		this.attemptNo++;
		this.videoObjectKey = requireNonNull(videoObjectKey);
		this.submittedAt = requireNonNull(now);
		this.reviewedAt = null;
		this.reviewNote = null;
	}

	public void approve(String note, Instant now) {
		state(isSubmitted(), "제출 상태의 수료만 승인할 수 있다");
		this.status = GraduationStatus.APPROVED;
		this.reviewedAt = requireNonNull(now);
		this.reviewNote = normalize(note);
		this.approvedAt = now;
	}

	public void requestRevision(String note, Instant now) {
		state(isSubmitted(), "제출 상태의 수료만 보완 요청할 수 있다");
		String normalized = normalize(note);
		isTrue(normalized != null, "보완 요청에는 메모가 필요하다");
		this.status = GraduationStatus.REVISION_REQUESTED;
		this.reviewedAt = requireNonNull(now);
		this.reviewNote = normalized;
	}

	public boolean isApproved() {
		return status == GraduationStatus.APPROVED;
	}

	public boolean isSubmitted() {
		return status == GraduationStatus.SUBMITTED;
	}

	private static String normalize(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		return note.strip();
	}
}
