package com.daesabu.meongcoach.partner.application;

import com.daesabu.meongcoach.partner.application.provided.PartnerBannerFinder;
import com.daesabu.meongcoach.partner.application.required.PartnerBannerRepository;
import com.daesabu.meongcoach.partner.domain.PartnerBanner;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerBannerQueryService implements PartnerBannerFinder {

	private static final ZoneId BANNER_ZONE = ZoneId.of("Asia/Seoul");

	private final PartnerBannerRepository partnerBannerRepository;

	@Override
	public List<PartnerBanner> findVisibleBanners() {
		return partnerBannerRepository.findVisibleOn(LocalDate.now(BANNER_ZONE));
	}
}
