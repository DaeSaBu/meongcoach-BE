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
@Table(name = "training_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingCategory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	public static TrainingCategory create(String title, int sortOrder) {
		TrainingCategory category = new TrainingCategory();
		category.title = title;
		category.sortOrder = sortOrder;
		return category;
	}
}
