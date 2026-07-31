package com.daesabu.meongcoach.progress.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 레슨 완료 원장. 행 생성이 곧 완료 이벤트라 createdAt이 완료 시각이다.
 *
 * @deprecated {@link UserLessonProgress}와 {@code UserCurriculumProgress}로 대체할 수 있다.
 */
@Deprecated
@Getter
@Entity
@Table(name = "lesson_completion_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonCompletionLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long lessonId;

	// 어떤 강아지와 수행했는지 기록한다
	@Column(nullable = false)
	private Long dogId;

	private LessonCompletionLog(LessonCompletionLogRecordCommand command) {
		this.userId = command.userId();
		this.lessonId = command.lessonId();
		this.dogId = command.dogId();
	}

	public static LessonCompletionLog record(LessonCompletionLogRecordCommand command) {
		return new LessonCompletionLog(command);
	}
}
