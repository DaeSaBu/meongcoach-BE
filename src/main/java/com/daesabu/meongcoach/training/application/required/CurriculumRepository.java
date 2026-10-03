package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	@Query("""
			select c
			from Curriculum c
			where c.topic.id = :topicId
			order by
				c.sortOrder asc,
				c.id asc
			""")
	@EntityGraph(attributePaths = "lessons")
	List<Curriculum> findAllByTopicId(Long topicId);
}
