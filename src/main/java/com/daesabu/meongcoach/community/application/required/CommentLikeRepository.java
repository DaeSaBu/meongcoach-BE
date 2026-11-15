package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.CommentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
	Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

	@Query("select l.commentId as targetId, count(l) as likeCount from CommentLike l where l.deletedAt is null and l.commentId in :targetIds group by l.commentId")
	List<LikeCount> countByTargetIds(List<Long> targetIds);

	@Query("select l.commentId from CommentLike l where l.deletedAt is null and l.commentId in :targetIds and l.userId = :userId")
	List<Long> findLikedTargetIds(Long userId, List<Long> targetIds);
}
