package com.daesabu.meongcoach.promotion.adapter.webapi.dto;

import com.daesabu.meongcoach.promotion.domain.PromotionBanner;
import java.time.LocalDate;

public record PromotionBannerResponse(Long id, String kind, String partnerName, String partnerKind, String title,
                                    String imageUrl, String imageBackground, String linkUrl, String linkLabel,
                                    String description, String benefit, String place, String validityLabel,
                                    String sharedCode, boolean disclosure, LocalDate startsOn, LocalDate endsOn) {

	public static PromotionBannerResponse from(PromotionBanner banner) {
		return new PromotionBannerResponse(banner.getId(), banner.getKind().name(), banner.getPartnerName(),
				banner.getPartnerKind(), banner.getTitle(), banner.getImageUrl(), banner.getImageBackground(),
				banner.getLinkUrl(), banner.getLinkLabel(), banner.getDescription(), banner.getBenefit(),
				banner.getPlace(), banner.getValidityLabel(), banner.getSharedCode(), banner.isDisclosure(),
				banner.getStartsOn(), banner.getEndsOn());
	}
}
