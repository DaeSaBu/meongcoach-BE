package com.daesabu.meongcoach.community.application.provided.dto;

import java.util.List;

public record CommentListResult(List<CommentResult> comments, Long nextCursor) {
}
