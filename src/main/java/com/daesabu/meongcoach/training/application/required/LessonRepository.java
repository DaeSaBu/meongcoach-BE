package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Lesson;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	@Override
	@EntityGraph(attributePaths = "curriculum.topic.trainingCategory")
	Optional<Lesson> findById(Long id);
}
