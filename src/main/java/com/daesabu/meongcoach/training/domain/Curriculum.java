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

	private Integer sortOrder;

	@Column(length = 512)
	private String thumbnailUrl;

	public static Curriculum create(Topic topic, String title, Integer sortOrder, String thumbnailUrl) {
		Curriculum curriculum = new Curriculum();
		curriculum.topic = topic;
		curriculum.title = title;
		curriculum.sortOrder = sortOrder;
		curriculum.thumbnailUrl = thumbnailUrl;
		return curriculum;
	}
}
