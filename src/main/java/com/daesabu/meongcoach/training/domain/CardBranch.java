package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 분기. 다음 카드는 그래프 순환을 객체 탐색으로 풀지 않도록 ID로만 참조한다.
 */
@Getter
@Entity
@Table(name = "card_branches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardBranch extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_id", nullable = false)
	private Card card;

	// 분기 종착 카드는 다음 카드가 없으므로 nullable
	@Column(name = "next_card_id")
	private Long nextCardId;

	public static CardBranch create(Card card, Long nextCardId) {
		CardBranch branch = new CardBranch();
		branch.card = card;
		branch.nextCardId = nextCardId;
		return branch;
	}
}
