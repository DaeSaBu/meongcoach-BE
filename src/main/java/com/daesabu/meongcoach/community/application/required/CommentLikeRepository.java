package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.CommentLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
	@Modifying
	@Query(value = "insert into comment_likes (comment_id, user_id) values (:targetId, :userId) on conflict (comment_id, user_id) do nothing", nativeQuery = true)
	void insertIfAbsent(Long targetId, Long userId);

	@Modifying
	@Query("delete from CommentLike l where l.commentId = :targetId and l.userId = :userId")
	void deleteLike(Long targetId, Long userId);

	@Query("select l.commentId as targetId, count(l) as likeCount from CommentLike l where l.commentId in :targetIds group by l.commentId")
	List<LikeCount> countByTargetIds(List<Long> targetIds);

	@Query("select l.commentId from CommentLike l where l.commentId in :targetIds and l.userId = :userId")
	List<Long> findLikedTargetIds(Long userId, List<Long> targetIds);
}
