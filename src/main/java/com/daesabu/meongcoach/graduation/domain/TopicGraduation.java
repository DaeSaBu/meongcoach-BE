package com.daesabu.meongcoach.graduation.domain;

import static java.util.Objects.requireNonNull;
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

	private Instant approvedAt;

	private Instant rewardClaimedAt;

	private Instant rewardExpiresAt;

	public static TopicGraduation start(Long userId, Long topicId) {
		TopicGraduation graduation = new TopicGraduation();
		graduation.userId = requireNonNull(userId);
		graduation.topicId = requireNonNull(topicId);
		graduation.status = GraduationStatus.SUBMITTED;
		return graduation;
	}

	public void resubmit() {
		state(!isApproved(), "승인된 수료에는 다시 제출할 수 없다");
		this.status = GraduationStatus.SUBMITTED;
	}

	public void approve(Instant now) {
		state(status == GraduationStatus.SUBMITTED, "제출 상태의 수료만 승인할 수 있다");
		this.status = GraduationStatus.APPROVED;
		this.approvedAt = requireNonNull(now);
	}

	public void requestRevision() {
		state(status == GraduationStatus.SUBMITTED, "제출 상태의 수료만 보완 요청할 수 있다");
		this.status = GraduationStatus.REVISION_REQUESTED;
	}

	public void claimReward(Instant claimedAt, Instant expiresAt) {
		state(isApproved(), "승인된 수료만 보상을 받을 수 있다");
		state(!isRewardClaimed(), "보상은 한 번만 받을 수 있다");
		this.rewardClaimedAt = requireNonNull(claimedAt);
		this.rewardExpiresAt = requireNonNull(expiresAt);
	}

	public boolean isApproved() {
		return status == GraduationStatus.APPROVED;
	}

	public boolean isRewardClaimed() {
		return rewardClaimedAt != null;
	}
}
