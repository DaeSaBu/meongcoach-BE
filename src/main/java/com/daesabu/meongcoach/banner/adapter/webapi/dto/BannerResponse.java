package com.daesabu.meongcoach.banner.adapter.webapi.dto;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.Instant;

public record BannerResponse(Long id, String type, String brandName, String brandCategory, String title,
                                    String imageUrl, String linkUrl, String linkLabel,
                                    String description, String benefit, String place,
                                    String sharedCode, Instant startsAt, Instant expiresAt) {

	public static BannerResponse from(Banner banner) {
		return new BannerResponse(banner.getId(), banner.getType().name(), banner.getBrandName(),
				banner.getBrandCategory(), banner.getTitle(), banner.getImageUrl(),
				banner.getLinkUrl(), banner.getLinkLabel(), banner.getDescription(), banner.getBenefit(),
				banner.getPlace(), banner.getSharedCode(),
				banner.getStartsAt(), banner.getExpiresAt());
	}
}
