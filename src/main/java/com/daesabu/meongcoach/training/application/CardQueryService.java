package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 조회 서비스. 다른 모듈이 카드 ID의 유효성을 확인하는 공개 API를 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardQueryService implements CardFinder {

	private final CardRepository cardRepository;

	@Override
	public boolean existsCard(Long cardId) {
		return cardRepository.existsById(cardId);
	}
}
