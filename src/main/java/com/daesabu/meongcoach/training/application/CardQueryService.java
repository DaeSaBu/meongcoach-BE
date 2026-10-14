package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardQueryService implements CardFinder {

	private final CardRepository cardRepository;

	@Override
	public List<CardSummaryResult> findSummaries(Collection<Long> cardIds) {
		if (cardIds.isEmpty()) {
			return List.of();
		}
		return cardRepository.findAllById(cardIds).stream()
				.map(CardSummaryResult::from)
				.toList();
	}
}
