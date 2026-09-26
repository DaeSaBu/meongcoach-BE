package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.comment.CardCommentLike;
import com.daesabu.meongcoach.training.domain.comment.CardCommentLikeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardCommentLikeRepository extends JpaRepository<CardCommentLike, CardCommentLikeId> {

	List<CardCommentLike> findAllByCardIdOrderByCommentIdAsc(Long cardId);

	@Modifying
	@Query(value = """
			INSERT INTO card_comment_likes (comment_id, user_id, card_id, created_at)
			SELECT id, :userId, card_id, CURRENT_TIMESTAMP
			FROM card_comments
			WHERE id = :commentId
			ON CONFLICT (comment_id, user_id) DO NOTHING
			""", nativeQuery = true)
	void like(@Param("commentId") Long commentId, @Param("userId") Long userId);

	@Modifying
	@Query(value = "DELETE FROM card_comment_likes WHERE comment_id = :commentId AND user_id = :userId",
			nativeQuery = true)
	void unlike(@Param("commentId") Long commentId, @Param("userId") Long userId);

	long countByCommentId(Long commentId);
}
