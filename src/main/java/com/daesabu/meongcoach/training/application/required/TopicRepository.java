package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	@Query("""
			select t
			from Topic t
			order by
				t.trainingCategory.sortOrder asc,
				t.trainingCategory.id asc,
				t.sortOrder asc,
				t.id asc
			""")
	List<Topic> findAllOrdered();

	@Query("""
			select t
			from Topic t
			join fetch t.trainingCategory
			order by
				t.trainingCategory.sortOrder asc,
				t.trainingCategory.id asc,
				t.sortOrder asc,
				t.id asc
			limit 1
			""")
	Optional<Topic> findFirstTopic();

	@Query("""
			select t
			from Topic t
			join fetch t.trainingCategory
			where t.id = :topicId
			""")
	Optional<Topic> findWithCategoryById(Long topicId);
}
