package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "lessons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "curriculum_id", nullable = false)
	private Curriculum curriculum;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private Integer estimatedMinutes;

	private Lesson(Curriculum curriculum, LessonCreateCommand command) {
		this.curriculum = curriculum;
		this.title = command.title();
		this.sortOrder = command.sortOrder();
		this.estimatedMinutes = command.estimatedMinutes();
	}

	public static Lesson create(Curriculum curriculum, LessonCreateCommand command) {
		return new Lesson(curriculum, command);
	}
}
