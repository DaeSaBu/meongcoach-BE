package com.daesabu.meongcoach.training.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
				name = "uk_curriculum_progress_user_dog_curriculum_trained_on",
				columnNames = {"user_id", "dog_id", "curriculum_id", "trained_on"})
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
	private LocalDate trainedOn;

	@Column(nullable = false)
	private boolean succeeded;

	public static CurriculumProgress register(CurriculumProgressRegisterCommand command) {
		CurriculumProgress curriculumProgress = new CurriculumProgress();

		curriculumProgress.userId = requireNonNull(command.userId());
		curriculumProgress.dogId = requireNonNull(command.dogId());
		curriculumProgress.curriculumId = requireNonNull(command.curriculumId());
		curriculumProgress.trainedOn = requireNonNull(command.trainedOn());
		curriculumProgress.succeeded = command.succeeded();

		return curriculumProgress;
	}

	public void succeed() {
		succeeded = true;
	}
}
