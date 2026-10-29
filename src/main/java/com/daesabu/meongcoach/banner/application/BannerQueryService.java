package com.daesabu.meongcoach.banner.application;

import com.daesabu.meongcoach.banner.application.provided.BannerFinder;
import com.daesabu.meongcoach.banner.application.required.BannerRepository;
import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerQueryService implements BannerFinder {

	private final BannerRepository bannerRepository;

	@Override
	public List<Banner> findVisibleBanners() {
		Instant now = Instant.now();
		return bannerRepository.findVisibleAt(now);
	}
}
