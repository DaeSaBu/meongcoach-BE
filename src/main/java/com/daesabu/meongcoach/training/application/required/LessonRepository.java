package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Lesson;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	@Query("""
			select l
			from Lesson l
			join fetch l.curriculum c
			join fetch c.topic t
			join fetch t.trainingCategory
			where l.id = :lessonId
			""")
	Optional<Lesson> findWithCategoryById(Long lessonId);
}
