package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Comment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	/**
	 * 게시글에 속하고 삭제되지 않은 댓글을 조회한다. 없거나 삭제됐거나 다른 게시글의 댓글이면 빈 Optional을 반환한다.
	 */
	Optional<Comment> findByIdAndPostIdAndDeletedAtIsNull(Long id, Long postId);
}
