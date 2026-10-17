package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	@Query("""
			select c
			from Curriculum c
			join fetch c.topic t
			join fetch t.trainingCategory
			where c.id = :curriculumId
			""")
	Optional<Curriculum> findWithCategoryById(Long curriculumId);
}
