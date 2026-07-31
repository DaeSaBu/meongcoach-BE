package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Lesson;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 레슨 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {

	/**
	 * 여러 커리큘럼의 레슨을 한 번에 조회한다. 정렬 순서 오름차순이며, 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<Lesson> findAllByCurriculum_IdInOrderBySortOrderAscIdAsc(Collection<Long> curriculumIds);

	/**
	 * 특정 커리큘럼의 레슨을 정렬 순서 오름차순으로 조회한다. 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<Lesson> findAllByCurriculum_IdOrderBySortOrderAscIdAsc(Long curriculumId);
}
