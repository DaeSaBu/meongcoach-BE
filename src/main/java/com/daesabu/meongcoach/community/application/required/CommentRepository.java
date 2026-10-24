package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Comment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	/**
	 * 게시글에 속하고 삭제되지 않은 댓글을 조회한다. 없거나 삭제됐거나 다른 게시글의 댓글이면 빈 Optional을 반환한다.
	 */
	Optional<Comment> findByIdAndPostIdAndDeletedAtIsNull(Long id, Long postId);

	/**
	 * 사용자가 작성하고 삭제되지 않은 댓글을 게시글이 삭제되지 않은 경우에만 조회한다.
	 * 없거나 삭제됐거나 다른 사용자의 댓글이거나 게시글이 삭제됐으면 빈 Optional을 반환한다.
	 */
	@Query("""
			select c
			from Comment c
			where c.id = :id
				and c.userId = :userId
				and c.deletedAt is null
				and c.post.deletedAt is null
			""")
	Optional<Comment> findActiveByIdAndUserId(Long id, Long userId);
}
