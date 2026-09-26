package com.daesabu.meongcoach.comment.application.required;

import com.daesabu.meongcoach.comment.domain.CardCommentLike;
import com.daesabu.meongcoach.comment.domain.CardCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentLikeRepository extends JpaRepository<CardCommentLike, CardCommentLikeId> {

	long countByCommentId(Long commentId);
}
