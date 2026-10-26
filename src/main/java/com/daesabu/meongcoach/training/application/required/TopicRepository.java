package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Topic;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	Sort EDUCATION_ORDER = Sort.by("trainingCategory.sortOrder", "trainingCategory.id", "sortOrder", "id");

	List<Topic> findAllByIdIn(Collection<Long> ids, Sort sort);

	@EntityGraph(attributePaths = "trainingCategory")
	Optional<Topic> findFirstBy(Sort sort);

	@Override
	@EntityGraph(attributePaths = "trainingCategory")
	Optional<Topic> findById(Long id);
}
