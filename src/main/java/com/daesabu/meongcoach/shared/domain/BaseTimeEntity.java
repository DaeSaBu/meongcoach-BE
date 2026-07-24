package com.daesabu.meongcoach.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 생성 시각만 기록하는 엔티티 공통 부모. 원장·로그 성격 테이블이 상속한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void initCreatedAt() {
		this.createdAt = LocalDateTime.now();
	}
}
