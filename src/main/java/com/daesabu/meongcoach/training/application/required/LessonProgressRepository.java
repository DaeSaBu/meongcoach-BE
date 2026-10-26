package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.LessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface LessonProgressRepository extends Repository<LessonProgress, Long> {

	LessonProgress save(LessonProgress progress);

	LessonProgress saveAndFlush(LessonProgress progress);

	List<LessonProgress> findAllByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

	Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

	long count();
}
