package com.daesabu.meongcoach.training.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "trained_lessons",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_trained_lessons_user_dog_lesson_trained_date_source",
				columnNames = {"user_id", "dog_id", "lesson_id", "trained_date", "source"}),
		indexes = @Index(
				name = "idx_trained_lessons_user_dog_trained_date",
				columnList = "user_id, dog_id, trained_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainedLesson extends BaseTimeEntity {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long dogId;

	@Column(nullable = false)
	private Long lessonId;

	@Column(nullable = false)
	private LocalDate trainedDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TrainingSource source;

	public static TrainedLesson record(Long userId, TrainedLessonRecordCommand command) {
		TrainedLesson trainedLesson = new TrainedLesson();

		trainedLesson.userId = requireNonNull(userId);
		trainedLesson.dogId = requireNonNull(command.dogId());
		trainedLesson.lessonId = requireNonNull(command.lessonId());
		trainedLesson.source = requireNonNull(command.source());
		trainedLesson.trainedDate = LocalDate.ofInstant(requireNonNull(command.completedAt()), SEOUL);

		return trainedLesson;
	}
}
