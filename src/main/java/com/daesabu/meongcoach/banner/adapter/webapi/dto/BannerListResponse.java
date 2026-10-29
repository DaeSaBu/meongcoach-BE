package com.daesabu.meongcoach.banner.adapter.webapi.dto;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.util.List;

public record BannerListResponse(List<BannerResponse> banners) {

	public static BannerListResponse from(List<Banner> banners) {
		return new BannerListResponse(banners.stream().map(BannerResponse::from).toList());
	}
}
