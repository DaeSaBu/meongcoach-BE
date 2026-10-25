package com.daesabu.meongcoach.community.application.provided.dto;

import java.util.List;

public record ReplyListResult(List<ReplyResult> replies, Long nextCursor) {
}
