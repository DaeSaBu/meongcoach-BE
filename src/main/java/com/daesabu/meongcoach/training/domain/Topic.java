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
@Table(name = "topics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "training_category_id", nullable = false)
	private TrainingCategory trainingCategory;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	private Topic(TrainingCategory trainingCategory, TopicCreateCommand command) {
		this.trainingCategory = trainingCategory;
		this.title = command.title();
		this.sortOrder = command.sortOrder();
	}

	public static Topic create(TrainingCategory trainingCategory, TopicCreateCommand command) {
		return new Topic(trainingCategory, command);
	}
}
