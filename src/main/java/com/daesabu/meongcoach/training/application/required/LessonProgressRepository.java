package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.LessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface LessonProgressRepository extends Repository<LessonProgress, Long> {

	LessonProgress save(LessonProgress progress);

	List<LessonProgress> findAllByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

	Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

	long count();

	/**
	 * 사용자·레슨의 진행 기록을 현재 트랜잭션이 끝날 때까지 잠근다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended('lesson-progress:' || :userId || ':' || :lessonId, 0))",
			nativeQuery = true)
	void lockProgress(Long userId, Long lessonId);
}
