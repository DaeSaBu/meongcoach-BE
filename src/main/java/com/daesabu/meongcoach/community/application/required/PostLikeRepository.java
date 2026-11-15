package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.PostLike;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
	@Modifying
	@Query(value = """
			insert into post_likes (post_id, user_id, created_at, updated_at)
			values (:targetId, :userId, :now, :now)
			on conflict (post_id, user_id) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(Long targetId, Long userId, LocalDateTime now);

	@Modifying
	@Query("update PostLike l set l.deletedAt = null, l.updatedAt = :now where l.postId = :targetId and l.userId = :userId and l.deletedAt is not null")
	void restore(Long targetId, Long userId, LocalDateTime now);

	@Modifying
	@Query("update PostLike l set l.deletedAt = :now, l.updatedAt = :now where l.postId = :targetId and l.userId = :userId and l.deletedAt is null")
	void softDelete(Long targetId, Long userId, LocalDateTime now);

	@Query("select l.postId as targetId, count(l) as likeCount from PostLike l where l.deletedAt is null and l.postId in :targetIds group by l.postId")
	List<LikeCount> countByTargetIds(List<Long> targetIds);

	@Query("select l.postId from PostLike l where l.deletedAt is null and l.postId in :targetIds and l.userId = :userId")
	List<Long> findLikedTargetIds(Long userId, List<Long> targetIds);
}
