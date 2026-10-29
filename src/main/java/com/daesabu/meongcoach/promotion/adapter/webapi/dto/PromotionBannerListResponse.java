package com.daesabu.meongcoach.promotion.adapter.webapi.dto;

import com.daesabu.meongcoach.promotion.domain.PromotionBanner;
import java.util.List;

public record PromotionBannerListResponse(List<PromotionBannerResponse> banners) {

	public static PromotionBannerListResponse from(List<PromotionBanner> banners) {
		return new PromotionBannerListResponse(banners.stream().map(PromotionBannerResponse::from).toList());
	}
}
