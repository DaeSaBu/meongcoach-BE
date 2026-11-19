package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.LessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface LessonProgressRepository extends Repository<LessonProgress, Long> {

	LessonProgress save(LessonProgress progress);

	LessonProgress saveAndFlush(LessonProgress progress);

	List<LessonProgress> findAllByUserIdAndDogIdAndLessonIdIn(Long userId, Long dogId, Collection<Long> lessonIds);

	Optional<LessonProgress> findByUserIdAndDogIdAndLessonId(Long userId, Long dogId, Long lessonId);

	long count();
}
