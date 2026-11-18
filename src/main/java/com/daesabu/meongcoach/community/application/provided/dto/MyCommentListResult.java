package com.daesabu.meongcoach.community.application.provided.dto;

import java.util.List;

public record MyCommentListResult(List<MyCommentResult> comments, Long nextCursor) {
}
