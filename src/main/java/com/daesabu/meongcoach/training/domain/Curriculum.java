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
@Table(name = "curriculums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Curriculum extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	// 썸네일 미등록 커리큘럼 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 512)
	private String thumbnailUrl;

	// 설명 없는 커리큘럼 허용 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	public static Curriculum create(Topic topic, String title, int sortOrder, String thumbnailUrl,
	                                String description) {
		Curriculum curriculum = new Curriculum();
		curriculum.topic = topic;
		curriculum.title = title;
		curriculum.sortOrder = sortOrder;
		curriculum.thumbnailUrl = Objects.requireNonNullElse(thumbnailUrl, "");
		curriculum.description = Objects.requireNonNullElse(description, "");
		return curriculum;
	}
}
