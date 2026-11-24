package com.daesabu.meongcoach.community.adapter.persistence;

import static com.daesabu.meongcoach.community.domain.QUserBlock.userBlock;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;

final class UserBlockPredicates {

	private UserBlockPredicates() {
	}

	static BooleanExpression notBlockedBy(Long viewerId, NumberPath<Long> authorId) {
		return JPAExpressions.selectOne()
				.from(userBlock)
				.where(userBlock.blockerId.eq(viewerId), userBlock.blockedId.eq(authorId))
				.notExists();
	}
}
