package com.daesabu.meongcoach.training.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "curriculum_progress",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_curriculum_progress_user_dog_curriculum_trained_date",
				columnNames = {"user_id", "dog_id", "curriculum_id", "trained_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurriculumProgress extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long dogId;

	@Column(nullable = false)
	private Long curriculumId;

	@Column(nullable = false)
	private LocalDate trainedDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CurriculumProgressStatus status;

	public static CurriculumProgress register(Long userId, CurriculumProgressRegisterCommand command) {
		CurriculumProgress curriculumProgress = new CurriculumProgress();

		curriculumProgress.userId = requireNonNull(userId);
		curriculumProgress.dogId = requireNonNull(command.dogId());
		curriculumProgress.curriculumId = requireNonNull(command.curriculumId());
		curriculumProgress.trainedDate = requireNonNull(command.trainedDate());
		curriculumProgress.status = CurriculumProgressStatus.PENDING;

		return curriculumProgress;
	}

	public void succeed() {
		state(status != CurriculumProgressStatus.SUCCEEDED, "이미 성공한 날입니다.");

		status = CurriculumProgressStatus.SUCCEEDED;
	}

	public void fail() {
		state(status != CurriculumProgressStatus.SUCCEEDED, "성공한 날은 실패로 바꿀 수 없습니다.");

		status = CurriculumProgressStatus.FAILED;
	}
}
