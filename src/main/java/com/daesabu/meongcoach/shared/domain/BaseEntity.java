package com.daesabu.meongcoach.shared.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

/**
 * 생성·수정 시각을 기록하는 엔티티 공통 부모.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void initUpdatedAt() {
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	void renewUpdatedAt() {
		this.updatedAt = LocalDateTime.now();
	}
}
