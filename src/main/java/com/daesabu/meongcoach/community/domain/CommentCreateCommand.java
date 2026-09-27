package com.daesabu.meongcoach.community.domain;

public record CommentCreateCommand(CommentTarget target, Long authorId, String content) {
}
