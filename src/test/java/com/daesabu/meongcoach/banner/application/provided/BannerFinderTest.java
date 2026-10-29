package com.daesabu.meongcoach.banner.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.banner.application.required.BannerRepository;
import com.daesabu.meongcoach.banner.domain.Banner;
import com.daesabu.meongcoach.banner.domain.BannerFixture;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class BannerFinderTest {

	private static final Instant NOW = Instant.now();

	@Autowired
	private BannerFinder bannerFinder;

	@Autowired
	private BannerRepository bannerRepository;

	@Test
	void 시작된_배너와_아직_만료되지_않은_배너를_보여_준다() {
		bannerRepository.save(BannerFixture.benefit("오늘 시작", NOW, NOW.plus(10, ChronoUnit.DAYS), 1));
		bannerRepository.save(BannerFixture.benefit("오늘 끝", NOW.minus(10, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), 2));

		assertThat(bannerFinder.findActiveBanners())
				.extracting(Banner::getTitle)
				.containsExactly("오늘 시작", "오늘 끝");
	}

	@Test
	void 기간이_시작되기_전이거나_끝난_배너는_보여_주지_않는다() {
		bannerRepository.save(BannerFixture.benefit("예약", NOW.plus(1, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS), 1));
		bannerRepository.save(BannerFixture.benefit("종료", NOW.minus(10, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), 2));

		assertThat(bannerFinder.findActiveBanners()).isEmpty();
	}

	@Test
	void 기간이_없는_배너는_상시_보여_준다() {
		bannerRepository.save(BannerFixture.introduction("상시 소개", 1));

		assertThat(bannerFinder.findActiveBanners())
				.extracting(Banner::getTitle)
				.containsExactly("상시 소개");
	}

	@Test
	void 숨긴_배너는_기간_안이어도_보여_주지_않는다() {
		bannerRepository.save(BannerFixture.inactive(BannerFixture.introduction("숨김", 1)));

		assertThat(bannerFinder.findActiveBanners()).isEmpty();
	}

	@Test
	void 정렬_순서가_작은_배너부터_같으면_먼저_등록한_배너부터_보여_준다() {
		bannerRepository.save(BannerFixture.introduction("둘째", 2));
		bannerRepository.save(BannerFixture.introduction("첫째 먼저", 1));
		bannerRepository.save(BannerFixture.introduction("첫째 나중", 1));

		assertThat(bannerFinder.findActiveBanners())
				.extracting(Banner::getTitle)
				.containsExactly("첫째 먼저", "첫째 나중", "둘째");
	}
	@Test
	void 시작_시각은_포함하고_만료_시각은_제외한다() {
		Instant boundary = Instant.parse("2026-12-31T15:00:00Z");
		bannerRepository.save(BannerFixture.benefit("시작", boundary, boundary.plusSeconds(60), 1));
		bannerRepository.save(BannerFixture.benefit("만료", boundary.minusSeconds(60), boundary, 2));

		assertThat(bannerRepository.findActiveBannersAt(boundary))
				.extracting(Banner::getTitle)
				.containsExactly("시작");
	}

}
