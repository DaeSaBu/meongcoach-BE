package com.daesabu.meongcoach.progress.domain;

import static java.util.Objects.requireNonNull;

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

@Getter
@Entity
@Table(
		name = "lesson_progress",
		uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonProgress extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long lessonId;

	@Column(nullable = false)
	private int completedCount;

	public static LessonProgress start(Long userId, Long lessonId) {
		LessonProgress lessonProgress = new LessonProgress();

		lessonProgress.userId = requireNonNull(userId);
		lessonProgress.lessonId = requireNonNull(lessonId);
		lessonProgress.completedCount = 0;

		return lessonProgress;
	}

	public void increaseCompletedCount() {
		this.completedCount++;
	}
}
