package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.domain.CommentTarget;
import com.daesabu.meongcoach.community.domain.CommentTargetType;
import com.daesabu.meongcoach.community.domain.exception.CommentTargetNotFoundException;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * 댓글 대상의 존재 확인. 대상을 소유한 모듈의 provided 인터페이스에 종류별로 묻는다. 새 대상은 여기에 매핑을 추가한다.
 */
@Component
class CommentTargetChecker {

	private final Map<CommentTargetType, Predicate<Long>> existence;

	CommentTargetChecker(CardFinder cardFinder) {
		this.existence = Map.of(CommentTargetType.CARD, cardFinder::existsCard);
	}

	void verify(CommentTarget target) {
		if (!existence.get(target.type()).test(target.id())) {
			throw new CommentTargetNotFoundException(target);
		}
	}
}
