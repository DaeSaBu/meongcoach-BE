package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
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

	// 설명 없는 카테고리 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	// 아이콘 미등록 카테고리 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 512)
	private String iconUrl;

	@Column(nullable = false)
	private int sortOrder;

	private TrainingCategory(String title, int sortOrder, String description, String iconUrl) {
		this.title = title;
		this.sortOrder = sortOrder;
		this.description = Objects.requireNonNullElse(description, "");
		this.iconUrl = Objects.requireNonNullElse(iconUrl, "");
	}

	public static TrainingCategory create(String title, int sortOrder, String description, String iconUrl) {
		return new TrainingCategory(title, sortOrder, description, iconUrl);
	}
}
