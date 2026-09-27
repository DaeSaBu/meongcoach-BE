package com.daesabu.meongcoach.cardcomment.application.required;

import com.daesabu.meongcoach.cardcomment.domain.CardCommentLike;
import com.daesabu.meongcoach.cardcomment.domain.CardCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentLikeRepository extends JpaRepository<CardCommentLike, CardCommentLikeId> {

	long countByCommentId(Long commentId);
}
