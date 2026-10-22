package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	@Override
	@EntityGraph(attributePaths = {"topic.trainingCategory", "expert"})
	Optional<Curriculum> findById(Long id);
}
