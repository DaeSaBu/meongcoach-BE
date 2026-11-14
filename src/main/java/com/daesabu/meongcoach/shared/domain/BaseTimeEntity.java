package com.daesabu.meongcoach.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void initCreatedAt() {
		this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
	}
}
