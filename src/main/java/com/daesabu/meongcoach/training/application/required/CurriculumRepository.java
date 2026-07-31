package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 커리큘럼 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	/**
	 * 특정 토픽의 커리큘럼을 정렬 순서 오름차순으로 조회한다. 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<Curriculum> findAllByTopic_IdOrderBySortOrderAscIdAsc(Long topicId);
}
