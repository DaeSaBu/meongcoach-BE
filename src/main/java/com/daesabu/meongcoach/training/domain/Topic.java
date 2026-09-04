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
import java.util.Objects;
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

	// 설명 없는 토픽 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	// 추가 설명 없는 토픽 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, columnDefinition = "TEXT")
	private String detail;

	// 아이콘 미등록 토픽 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 512)
	private String iconUrl;

	@Column(nullable = false)
	private int sortOrder;

	private Topic(TrainingCategory trainingCategory, TopicCreateCommand command) {
		this.trainingCategory = trainingCategory;
		this.title = command.title();
		this.sortOrder = command.sortOrder();
		this.description = Objects.requireNonNullElse(command.description(), "");
		this.detail = Objects.requireNonNullElse(command.detail(), "");
		this.iconUrl = Objects.requireNonNullElse(command.iconUrl(), "");
	}

	public static Topic create(TrainingCategory trainingCategory, TopicCreateCommand command) {
		return new Topic(trainingCategory, command);
	}
}
