package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;

public record PostResult(Post post, UserProfileSummaryResult author, CardSummaryResult mentionedCard) {
}
