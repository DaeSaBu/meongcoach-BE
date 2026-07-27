package com.daesabu.meongcoach.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
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

	/**
	 * 바뀔 필드가 없어 더티 체킹이 일어나지 않는 변경에서 수정 시각만 직접 갱신한다.
	 */
	protected void touch() {
		this.updatedAt = LocalDateTime.now();
	}
}
