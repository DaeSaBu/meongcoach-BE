package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CardRepository extends JpaRepository<Card, Long> {

	@Query("""
			select c
			from Card c
			where c.lesson.id = :lessonId
			order by
				c.sortOrder asc,
				c.id asc
			""")
	@EntityGraph(attributePaths = "cardMedia")
	List<Card> findAllByLessonId(Long lessonId);
}
