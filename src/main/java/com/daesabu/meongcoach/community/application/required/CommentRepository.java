package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.application.provided.CommentCountResult;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentTarget;
import com.daesabu.meongcoach.community.domain.CommentTargetType;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	// 답글 수·좋아요 수·내 좋아요 여부를 서브쿼리로 함께 집계한다. 작성자 닉네임은 user 모듈에서 따로 받아 합친다
	String SUMMARY_SELECT = """
			select new com.daesabu.meongcoach.community.application.required.CommentSummary(
				c.id, c.parentId, c.authorId, c.content, c.createdAt,
				(select count(r) from Comment r where r.rootId = c.id),
				(select count(l) from CommentLike l where l.commentId = c.id),
				exists (select 1 from CommentLike m where m.commentId = c.id and m.userId = :userId))
			from Comment c
			""";

	/**
	 * 대상의 최상위 댓글을 id 내림차순(최신순)으로 한 페이지 읽는다. cursor가 null이면 첫 페이지다.
	 */
	default Slice<CommentSummary> findTopLevel(CommentTarget target, Long userId, Long cursor, Pageable pageable) {
		return findTopLevel(target.type(), target.id(), userId, cursor, pageable);
	}

	@Query(SUMMARY_SELECT + "where c.target.type = :targetType and c.target.id = :targetId and c.rootId is null "
			+ "and (:cursor is null or c.id < :cursor) order by c.id desc")
	Slice<CommentSummary> findTopLevel(@Param("targetType") CommentTargetType targetType,
	                                   @Param("targetId") Long targetId, @Param("userId") Long userId,
	                                   @Param("cursor") Long cursor, Pageable pageable);

	/**
	 * 스레드의 답글을 id 오름차순(오래된 순)으로 한 페이지 읽는다. cursor가 null이면 첫 페이지다.
	 */
	@Query(SUMMARY_SELECT + "where c.rootId = :rootId "
			+ "and (:cursor is null or c.id > :cursor) order by c.id asc")
	Slice<CommentSummary> findReplies(@Param("rootId") Long rootId, @Param("userId") Long userId,
	                                  @Param("cursor") Long cursor, Pageable pageable);

	default long countByTarget(CommentTarget target) {
		return countByTarget_TypeAndTarget_Id(target.type(), target.id());
	}

	long countByTarget_TypeAndTarget_Id(CommentTargetType type, Long id);

	long countByRootId(Long rootId);

	@Query("select new com.daesabu.meongcoach.community.application.provided.CommentCountResult(c.target.id, count(c)) "
			+ "from Comment c where c.target.type = :type and c.target.id in :ids group by c.target.id")
	List<CommentCountResult> countByTargetIds(@Param("type") CommentTargetType type, @Param("ids") Set<Long> ids);

	default Comment getComment(Long id) {
		return findById(id).orElseThrow(() -> new CommentNotFoundException(id));
	}
}
