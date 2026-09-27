package com.daesabu.meongcoach.comment.application.required;

import com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult;
import com.daesabu.meongcoach.comment.domain.CardComment;
import com.daesabu.meongcoach.comment.domain.exception.CommentNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

	// 답글 수·좋아요 수·내 좋아요 여부를 서브쿼리로 함께 집계한다. 작성자 닉네임은 user 모듈에서 따로 받아 합친다
	String SUMMARY_SELECT = """
			select new com.daesabu.meongcoach.comment.application.required.CardCommentSummary(
				c.id, c.parentId, c.authorId, c.content, c.createdAt,
				(select count(r) from CardComment r where r.rootId = c.id),
				(select count(l) from CardCommentLike l where l.commentId = c.id),
				exists (select 1 from CardCommentLike m where m.commentId = c.id and m.userId = :userId))
			from CardComment c
			""";

	/**
	 * 카드의 최상위 댓글을 id 내림차순(최신순)으로 한 페이지 읽는다. cursor가 null이면 첫 페이지다.
	 */
	@Query(SUMMARY_SELECT + "where c.cardId = :cardId and c.rootId is null "
			+ "and (:cursor is null or c.id < :cursor) order by c.id desc")
	Slice<CardCommentSummary> findTopLevel(@Param("cardId") Long cardId, @Param("userId") Long userId,
	                                       @Param("cursor") Long cursor, Pageable pageable);

	/**
	 * 스레드의 답글을 id 오름차순(오래된 순)으로 한 페이지 읽는다. cursor가 null이면 첫 페이지다.
	 */
	@Query(SUMMARY_SELECT + "where c.rootId = :rootId "
			+ "and (:cursor is null or c.id > :cursor) order by c.id asc")
	Slice<CardCommentSummary> findReplies(@Param("rootId") Long rootId, @Param("userId") Long userId,
	                                      @Param("cursor") Long cursor, Pageable pageable);

	long countByCardId(Long cardId);

	long countByRootId(Long rootId);

	@Query("select new com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult(c.cardId, count(c)) "
			+ "from CardComment c where c.cardId in :cardIds group by c.cardId")
	List<CardCommentCountResult> countByCardIds(@Param("cardIds") Set<Long> cardIds);

	default CardComment getComment(Long id) {
		return findById(id).orElseThrow(() -> new CommentNotFoundException(id));
	}
}
