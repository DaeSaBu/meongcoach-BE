package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.CardCommentLike;
import com.daesabu.meongcoach.community.domain.CardCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentLikeRepository extends JpaRepository<CardCommentLike, CardCommentLikeId> {

	long countByCommentId(Long commentId);
}
