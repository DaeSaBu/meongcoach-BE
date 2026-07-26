package com.daesabu.meongcoach.progress.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 레슨 진행 상태. completedCount는 반복 완료 횟수 캐시(U-0207)이며 원천은 완료 원장이다.
 */
@Getter
@Entity
@Table(
		name = "user_lesson_progress",
		uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLessonProgress extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long lessonId;

	@Column(nullable = false)
	private int completedCount;

	public static UserLessonProgress start(Long userId, Long lessonId) {
		UserLessonProgress progress = new UserLessonProgress();
		progress.userId = userId;
		progress.lessonId = lessonId;
		progress.completedCount = 0;
		return progress;
	}

	public void increaseCompletedCount() {
		this.completedCount++;
	}
}
