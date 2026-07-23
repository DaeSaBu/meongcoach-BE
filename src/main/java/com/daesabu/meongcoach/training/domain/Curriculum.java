package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	// 프리미엄 훈련은 결제 플로우로 연결된다 (U-0212)
	@Column(nullable = false)
	private boolean isPremium;

	// null이면 전체 노출, 값이 있으면 보유 강아지 size 집합에 포함될 때만 노출
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private TargetDogSize targetDogSize;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CurriculumStatus status;

	public static Curriculum create(Topic topic, String title, Integer sortOrder, String thumbnailUrl,
	                                boolean isPremium, TargetDogSize targetDogSize) {
		Curriculum curriculum = new Curriculum();
		curriculum.topic = topic;
		curriculum.title = title;
		curriculum.sortOrder = sortOrder;
		curriculum.thumbnailUrl = thumbnailUrl;
		curriculum.isPremium = isPremium;
		curriculum.targetDogSize = targetDogSize;
		curriculum.status = CurriculumStatus.ACTIVE;
		return curriculum;
	}

	public void deactivate() {
		this.status = CurriculumStatus.INACTIVE;
	}
}
