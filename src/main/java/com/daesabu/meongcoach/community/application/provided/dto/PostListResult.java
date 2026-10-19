package com.daesabu.meongcoach.community.application.provided.dto;

import java.util.List;

public record PostListResult(List<PostResult> posts, Long nextCursor) {
}
