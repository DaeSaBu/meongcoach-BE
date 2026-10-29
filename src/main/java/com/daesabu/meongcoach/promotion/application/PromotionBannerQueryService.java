package com.daesabu.meongcoach.promotion.application;

import com.daesabu.meongcoach.promotion.application.provided.PromotionBannerFinder;
import com.daesabu.meongcoach.promotion.application.required.PromotionBannerRepository;
import com.daesabu.meongcoach.promotion.domain.PromotionBanner;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionBannerQueryService implements PromotionBannerFinder {

	private static final ZoneId BANNER_ZONE = ZoneId.of("Asia/Seoul");

	private final PromotionBannerRepository promotionBannerRepository;

	@Override
	public List<PromotionBanner> findVisibleBanners() {
		return promotionBannerRepository.findVisibleOn(LocalDate.now(BANNER_ZONE));
	}
}
