package com.daesabu.meongcoach.banner.adapter.webapi;

import com.daesabu.meongcoach.banner.adapter.webapi.dto.BannerListResponse;
import com.daesabu.meongcoach.banner.application.provided.BannerFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

	private final BannerFinder bannerFinder;

	@GetMapping
	public BannerListResponse findBanners() {
		return BannerListResponse.from(bannerFinder.findVisibleBanners());
	}
}
