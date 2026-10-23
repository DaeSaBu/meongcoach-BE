package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;

public record ReplyResult(Comment comment, UserProfileSummaryResult author) {
}
