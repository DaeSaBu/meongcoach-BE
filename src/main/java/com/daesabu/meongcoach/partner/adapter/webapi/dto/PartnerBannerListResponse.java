package com.daesabu.meongcoach.partner.adapter.webapi.dto;

import com.daesabu.meongcoach.partner.domain.PartnerBanner;
import java.util.List;

public record PartnerBannerListResponse(List<PartnerBannerResponse> banners) {

	public static PartnerBannerListResponse from(List<PartnerBanner> banners) {
		return new PartnerBannerListResponse(banners.stream().map(PartnerBannerResponse::from).toList());
	}
}
