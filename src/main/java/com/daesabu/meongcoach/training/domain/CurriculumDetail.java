package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "curriculum_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurriculumDetail extends BaseEntity {

	@Id
	private Long curriculumId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "curriculum_id")
	private Curriculum curriculum;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Difficulty difficulty;

	public static CurriculumDetail create(Curriculum curriculum, String description, Difficulty difficulty) {
		CurriculumDetail detail = new CurriculumDetail();
		detail.curriculum = curriculum;
		detail.description = description;
		detail.difficulty = difficulty;
		return detail;
	}
}
