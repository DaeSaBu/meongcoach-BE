package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

public interface CurriculumProgressRepository extends Repository<CurriculumProgress, Long> {

	CurriculumProgress saveAndFlush(CurriculumProgress curriculumProgress);

	boolean existsByUserIdAndDogIdAndCurriculumIdAndTrainedDate(Long userId, Long dogId, Long curriculumId,
			LocalDate trainedDate);

	Optional<CurriculumProgress> findByUserIdAndDogIdAndCurriculumIdAndTrainedDate(Long userId, Long dogId,
			Long curriculumId, LocalDate trainedDate);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<CurriculumProgress> findLockedByUserIdAndDogIdAndCurriculumIdAndTrainedDate(Long userId, Long dogId,
			Long curriculumId, LocalDate trainedDate);

	List<CurriculumProgress> findAllByUserIdAndDogIdAndTrainedDateBetween(Long userId, Long dogId, LocalDate from,
			LocalDate to);

	long count();
}
