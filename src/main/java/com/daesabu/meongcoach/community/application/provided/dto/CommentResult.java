package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;

public record CommentResult(Comment comment, UserProfileSummaryResult author) {
}
