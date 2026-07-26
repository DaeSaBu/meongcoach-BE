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
 * 사용자별 토픽 진입 기록. updatedAt으로 커리큘럼 진입 시 최근 L3 분기(U-0201)를 판단한다.
 */
@Getter
@Entity
@Table(
		name = "user_curriculums_progress",
		uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCurriculumProgress extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long topicId;

	public static UserCurriculumProgress enter(Long userId, Long topicId) {
		UserCurriculumProgress progress = new UserCurriculumProgress();
		progress.userId = userId;
		progress.topicId = topicId;
		return progress;
	}
}
