package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMentionTargetVerifier implements MentionTargetVerifier {

	private final CardFinder cardFinder;

	@Override
	public MentionTargetType targetType() {
		return MentionTargetType.CARD;
	}

	@Override
	public boolean exists(Long targetId) {
		List<CardSummaryResult> cards = cardFinder.findSummaries(List.of(targetId));
		return !cards.isEmpty();
	}
}
