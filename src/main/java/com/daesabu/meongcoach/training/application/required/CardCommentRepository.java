package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.comment.CardComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

	List<CardComment> findByCardIdAndParentIdIsNullOrderByIdDesc(Long cardId);

	List<CardComment> findByCardIdAndParentIdIsNotNull(Long cardId);

	List<CardComment> findByParentIdOrderByIdAsc(Long parentId);

	long countByCardId(Long cardId);

	boolean existsByIdAndParentIdIsNull(Long id);
}
