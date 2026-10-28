package com.daesabu.meongcoach.partner.adapter.webapi;

import com.daesabu.meongcoach.partner.adapter.webapi.dto.PartnerBannerListResponse;
import com.daesabu.meongcoach.partner.application.provided.PartnerBannerFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerBannerController {

	private final PartnerBannerFinder partnerBannerFinder;

	@GetMapping("/banners")
	public PartnerBannerListResponse findBanners() {
		return PartnerBannerListResponse.from(partnerBannerFinder.findVisibleBanners());
	}
}
