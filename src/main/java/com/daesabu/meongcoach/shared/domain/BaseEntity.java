package com.daesabu.meongcoach.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void initUpdatedAt() {
		renewUpdatedAt();
	}

	@PreUpdate
	protected void renewUpdatedAt() {
		this.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
	}
}
