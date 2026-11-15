package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.PostLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
	Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

	@Query("""
			select l.postId as targetId, count(l) as likeCount
			from PostLike l
			where l.deletedAt is null and l.postId in :targetIds
			group by l.postId
			""")
	List<LikeCount> countByTargetIds(List<Long> targetIds);

	@Query("""
			select l.postId
			from PostLike l
			where l.deletedAt is null and l.postId in :targetIds and l.userId = :userId
			""")
	List<Long> findLikedTargetIds(Long userId, List<Long> targetIds);
}
