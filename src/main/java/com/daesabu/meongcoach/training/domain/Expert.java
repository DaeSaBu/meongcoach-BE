package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "experts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expert extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, length = 512)
	private String imageUrl;
}
