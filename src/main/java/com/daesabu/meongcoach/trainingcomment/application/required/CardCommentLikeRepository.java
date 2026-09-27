package com.daesabu.meongcoach.trainingcomment.application.required;

import com.daesabu.meongcoach.trainingcomment.domain.CardCommentLike;
import com.daesabu.meongcoach.trainingcomment.domain.CardCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentLikeRepository extends JpaRepository<CardCommentLike, CardCommentLikeId> {

	long countByCommentId(Long commentId);
}
