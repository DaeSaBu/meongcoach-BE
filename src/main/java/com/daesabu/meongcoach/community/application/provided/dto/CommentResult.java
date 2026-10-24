package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.Comment;

public record CommentResult(Comment comment, AuthorResult author, long replyCount) {
}
