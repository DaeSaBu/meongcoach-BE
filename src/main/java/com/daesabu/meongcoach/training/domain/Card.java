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

/**
 * 레슨 내 훈련 카드. 레슨(L4)당 4~9개 SKILL 규칙이 있다 (U-0205).
 */
@Getter
@Entity
@Table(name = "cards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@Column(length = 200)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	// 영상/이미지만 있는 카드를 허용하므로 nullable
	@Column(columnDefinition = "TEXT")
	private String instruction;

	public static Card create(Lesson lesson, String title, int sortOrder, String instruction) {
		Card card = new Card();
		card.lesson = lesson;
		card.title = title;
		card.sortOrder = sortOrder;
		card.instruction = instruction;
		return card;
	}
}
