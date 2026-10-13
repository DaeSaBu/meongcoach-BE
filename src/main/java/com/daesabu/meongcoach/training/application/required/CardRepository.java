package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CardRepository extends JpaRepository<Card, Long> {

	@Query("""
			select c
			from Card c
			join fetch c.lesson l
			join fetch l.curriculum cu
			join fetch cu.topic t
			join fetch t.trainingCategory
			where c.id in :cardIds
			""")
	List<Card> findAllWithHierarchyByIdIn(Collection<Long> cardIds);
}
