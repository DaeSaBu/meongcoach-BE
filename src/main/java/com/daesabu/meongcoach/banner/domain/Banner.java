package com.daesabu.meongcoach.banner.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "banners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BannerType type;

	@Column(nullable = false, length = 50)
	private String brandName;

	// 업종
	@Column(length = 30)
	private String brandCategory;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 512)
	private String imageUrl;

	@Column(nullable = false, length = 1024)
	private String linkUrl;

	@Column(length = 30)
	private String linkLabel;

	@Column(length = 500)
	private String brandDescription;

	@Column(length = 100)
	private String benefitDescription;

	@Column(length = 100)
	private String usageLocation;

	@Column(length = 50)
	private String promotionCode;

	private Instant startsAt;

	private Instant expiresAt;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active;
}
