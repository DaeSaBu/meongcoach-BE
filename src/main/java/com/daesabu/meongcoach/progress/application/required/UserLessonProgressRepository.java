package com.daesabu.meongcoach.progress.application.required;

import com.daesabu.meongcoach.progress.domain.UserLessonProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자별 레슨 진행도 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {

	/**
	 * 한 사용자의 여러 레슨 진행도를 한 번에 조회한다.
	 */
	List<UserLessonProgress> findAllByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

	/**
	 * 한 사용자의 특정 레슨 진행도를 조회한다. 기록이 없으면 빈 Optional을 반환한다.
	 */
	Optional<UserLessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
}
