package com.daesabu.meongcoach.banner.adapter.webapi.dto;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.LocalDate;

public record BannerResponse(Long id, String type, String brandName, String brandCategory, String title,
                                    String imageUrl, String imageBackground, String linkUrl, String linkLabel,
                                    String description, String benefit, String place, String validityLabel,
                                    String sharedCode, boolean disclosure, LocalDate startsOn, LocalDate endsOn) {

	public static BannerResponse from(Banner banner) {
		return new BannerResponse(banner.getId(), banner.getType().name(), banner.getBrandName(),
				banner.getBrandCategory(), banner.getTitle(), banner.getImageUrl(), banner.getImageBackground(),
				banner.getLinkUrl(), banner.getLinkLabel(), banner.getDescription(), banner.getBenefit(),
				banner.getPlace(), banner.getValidityLabel(), banner.getSharedCode(), banner.isDisclosure(),
				banner.getStartsOn(), banner.getEndsOn());
	}
}
