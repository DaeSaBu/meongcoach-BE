package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.Comment;

public record MyCommentResult(Comment comment, long likeCount, boolean likedByMe) {
}
