package com.daesabu.meongcoach.banner.application;

import com.daesabu.meongcoach.banner.application.provided.BannerFinder;
import com.daesabu.meongcoach.banner.application.required.BannerRepository;
import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerQueryService implements BannerFinder {

	private static final ZoneId BANNER_ZONE = ZoneId.of("Asia/Seoul");

	private final BannerRepository bannerRepository;

	@Override
	public List<Banner> findVisibleBanners() {
		return bannerRepository.findVisibleOn(LocalDate.now(BANNER_ZONE));
	}
}
