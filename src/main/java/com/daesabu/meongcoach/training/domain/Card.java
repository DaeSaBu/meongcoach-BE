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

	// 영상/이미지만 있는 카드는 제목이 없을 수 있다 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	// 영상/이미지만 있는 카드를 허용한다 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, columnDefinition = "TEXT")
	private String instruction;

	private Card(Lesson lesson, CardCreateCommand command) {
		this.lesson = lesson;
		this.title = Objects.requireNonNullElse(command.title(), "");
		this.sortOrder = command.sortOrder();
		this.instruction = Objects.requireNonNullElse(command.instruction(), "");
	}

	public static Card create(Lesson lesson, CardCreateCommand command) {
		return new Card(lesson, command);
	}
}
