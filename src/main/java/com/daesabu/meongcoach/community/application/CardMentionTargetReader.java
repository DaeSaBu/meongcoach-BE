package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMentionTargetReader implements MentionTargetReader {

	private final CardFinder cardFinder;

	@Override
	public MentionTargetType targetType() {
		return MentionTargetType.CARD;
	}

	@Override
	public List<MentionTargetResult> findSummaries(Collection<Long> targetIds) {
		return cardFinder.findSummaries(targetIds).stream()
				.map(card -> new MentionTargetResult(MentionTargetType.CARD, card.id(), card.title()))
				.toList();
	}
}
