package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.CommentLike;
import com.daesabu.meongcoach.community.domain.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

	long countByCommentId(Long commentId);
}
