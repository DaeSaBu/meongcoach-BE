package com.daesabu.meongcoach.promotion.adapter.webapi;

import com.daesabu.meongcoach.promotion.adapter.webapi.dto.PromotionBannerListResponse;
import com.daesabu.meongcoach.promotion.application.provided.PromotionBannerFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PromotionBannerController {

	private final PromotionBannerFinder promotionBannerFinder;

	@GetMapping("/banners")
	public PromotionBannerListResponse findBanners() {
		return PromotionBannerListResponse.from(promotionBannerFinder.findVisibleBanners());
	}
}
