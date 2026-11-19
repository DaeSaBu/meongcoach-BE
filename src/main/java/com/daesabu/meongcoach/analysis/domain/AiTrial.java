package com.daesabu.meongcoach.analysis.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Getter
@Entity
@Table(
		name = "ai_trials",
		uniqueConstraints = @UniqueConstraint(name = "uk_ai_trials_user_id", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTrial extends BaseEntity {

	public static final int DEFAULT_GRANTED_COUNT = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private int grantedCount;

	@Column(nullable = false)
	private int usedCount;

	public static AiTrial start(Long userId) {
		AiTrial trial = new AiTrial();

		trial.userId = requireNonNull(userId);
		trial.grantedCount = DEFAULT_GRANTED_COUNT;
		trial.usedCount = 0;

		return trial;
	}

	public void use() {
		usedCount++;
	}

	public void grant(int count) {
		Assert.state(count > 0, "추가 부여 횟수는 1 이상이어야 한다");

		grantedCount += count;
	}

	public int maxCount() {
		return grantedCount;
	}

	public int remainingCount() {
		return Math.max(0, grantedCount - usedCount);
	}

	public boolean isAvailable() {
		return usedCount < grantedCount;
	}
}
