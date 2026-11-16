package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.TrainedLesson;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface TrainedLessonRepository extends Repository<TrainedLesson, Long> {

	TrainedLesson saveAndFlush(TrainedLesson trainedLesson);

	boolean existsByUserIdAndDogIdAndLessonIdAndTrainedDateAndSource(Long userId, Long dogId, Long lessonId,
			LocalDate trainedDate, TrainingSource source);

	List<TrainedLesson> findAllByUserIdAndDogIdAndTrainedDateBetween(Long userId, Long dogId, LocalDate from,
			LocalDate to);

	List<TrainedLesson> findAllByUserIdAndDogIdAndLessonIdInAndTrainedDateBefore(Long userId, Long dogId,
			Collection<Long> lessonIds, LocalDate trainedDate);

	Optional<TrainedLesson> findFirstByUserIdAndDogIdOrderByTrainedDateAsc(Long userId, Long dogId);

	long count();
}
