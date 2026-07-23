package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Getter
@Entity
@Table(name = "card_media")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardMedia extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_id", nullable = false)
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MediaType mediaType;

	@Column(nullable = false, length = 512)
	private String url;

	@Column(nullable = false)
	private int sortOrder;

	public static CardMedia create(Card card, MediaType mediaType, String url, int sortOrder) {
		CardMedia media = new CardMedia();
		media.card = card;
		media.mediaType = mediaType;
		media.url = url;
		media.sortOrder = sortOrder;
		return media;
	}
}
