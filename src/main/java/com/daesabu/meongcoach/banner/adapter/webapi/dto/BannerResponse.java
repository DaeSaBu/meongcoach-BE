package com.daesabu.meongcoach.banner.adapter.webapi.dto;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.Instant;

public record BannerResponse(Long id, String type, String brandName, String brandCategory, String title,
                                    String imageUrl, String linkUrl, String linkLabel,
                                    String brandDescription, String benefitDescription, String usageLocation,
                                    String promotionCode, Instant startsAt, Instant expiresAt) {

	public static BannerResponse from(Banner banner) {
		return new BannerResponse(banner.getId(), banner.getType().name(), banner.getBrandName(),
				banner.getBrandCategory(), banner.getTitle(), banner.getImageUrl(),
				banner.getLinkUrl(), banner.getLinkLabel(), banner.getBrandDescription(), banner.getBenefitDescription(),
				banner.getUsageLocation(), banner.getPromotionCode(),
				banner.getStartsAt(), banner.getExpiresAt());
	}
}
