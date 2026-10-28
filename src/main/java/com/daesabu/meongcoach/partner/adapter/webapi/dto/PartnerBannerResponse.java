package com.daesabu.meongcoach.partner.adapter.webapi.dto;

import com.daesabu.meongcoach.partner.domain.PartnerBanner;
import java.time.LocalDate;

public record PartnerBannerResponse(Long id, String kind, String partnerName, String partnerKind, String title,
                                    String imageUrl, String imageBackground, String linkUrl, String linkLabel,
                                    String description, String benefit, String place, String validityLabel,
                                    String sharedCode, boolean disclosure, LocalDate startsOn, LocalDate endsOn) {

	public static PartnerBannerResponse from(PartnerBanner banner) {
		return new PartnerBannerResponse(banner.getId(), banner.getKind().name(), banner.getPartnerName(),
				banner.getPartnerKind(), banner.getTitle(), banner.getImageUrl(), banner.getImageBackground(),
				banner.getLinkUrl(), banner.getLinkLabel(), banner.getDescription(), banner.getBenefit(),
				banner.getPlace(), banner.getValidityLabel(), banner.getSharedCode(), banner.isDisclosure(),
				banner.getStartsOn(), banner.getEndsOn());
	}
}
