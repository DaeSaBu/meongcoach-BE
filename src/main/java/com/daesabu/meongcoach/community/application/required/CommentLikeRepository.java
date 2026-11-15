package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.CommentLike;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
	@Modifying
	@Query(value = """
			insert into comment_likes (comment_id, user_id, created_at, updated_at)
			values (:targetId, :userId, :now, :now)
			on conflict (comment_id, user_id) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(Long targetId, Long userId, LocalDateTime now);

	@Modifying
	@Query("update CommentLike l set l.deletedAt = null, l.updatedAt = :now where l.commentId = :targetId and l.userId = :userId and l.deletedAt is not null")
	void restore(Long targetId, Long userId, LocalDateTime now);

	@Modifying
	@Query("update CommentLike l set l.deletedAt = :now, l.updatedAt = :now where l.commentId = :targetId and l.userId = :userId and l.deletedAt is null")
	void softDelete(Long targetId, Long userId, LocalDateTime now);

	@Query("select l.commentId as targetId, count(l) as likeCount from CommentLike l where l.deletedAt is null and l.commentId in :targetIds group by l.commentId")
	List<LikeCount> countByTargetIds(List<Long> targetIds);

	@Query("select l.commentId from CommentLike l where l.deletedAt is null and l.commentId in :targetIds and l.userId = :userId")
	List<Long> findLikedTargetIds(Long userId, List<Long> targetIds);
}
