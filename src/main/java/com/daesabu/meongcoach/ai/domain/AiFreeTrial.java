package com.daesabu.meongcoach.ai.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 무료 체험 횟수. 소진 시 기능이 잠긴다 (U-0403). user는 타 모듈이라 객체 참조 없이 userId를 PK로 공유한다.
 */
@Getter
@Entity
@Table(name = "ai_free_trials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFreeTrial extends BaseEntity {

	@Id
	private Long userId;

	@Column(nullable = false)
	private int usedCount;

	public static AiFreeTrial init(Long userId) {
		AiFreeTrial trial = new AiFreeTrial();
		trial.userId = userId;
		trial.usedCount = 0;
		return trial;
	}

	public void use() {
		this.usedCount++;
	}
}
