package com.daesabu.meongcoach.promotion.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.promotion.application.required.PromotionBannerRepository;
import com.daesabu.meongcoach.promotion.domain.PromotionBanner;
import com.daesabu.meongcoach.promotion.domain.PromotionBannerFixture;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PromotionBannerFinderTest {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

	@Autowired
	private PromotionBannerFinder promotionBannerFinder;

	@Autowired
	private PromotionBannerRepository promotionBannerRepository;

	@Test
	void 노출_기간의_양_끝_날짜에도_배너를_보여_준다() {
		promotionBannerRepository.save(PromotionBannerFixture.benefit("오늘 시작", TODAY, TODAY.plusDays(10), 1));
		promotionBannerRepository.save(PromotionBannerFixture.benefit("오늘 끝", TODAY.minusDays(10), TODAY, 2));

		assertThat(promotionBannerFinder.findVisibleBanners())
				.extracting(PromotionBanner::getTitle)
				.containsExactly("오늘 시작", "오늘 끝");
	}

	@Test
	void 기간이_시작되기_전이거나_끝난_배너는_보여_주지_않는다() {
		promotionBannerRepository.save(PromotionBannerFixture.benefit("예약", TODAY.plusDays(1), TODAY.plusDays(10), 1));
		promotionBannerRepository.save(PromotionBannerFixture.benefit("종료", TODAY.minusDays(10), TODAY.minusDays(1), 2));

		assertThat(promotionBannerFinder.findVisibleBanners()).isEmpty();
	}

	@Test
	void 기간이_없는_배너는_상시_보여_준다() {
		promotionBannerRepository.save(PromotionBannerFixture.introduction("상시 소개", 1));

		assertThat(promotionBannerFinder.findVisibleBanners())
				.extracting(PromotionBanner::getTitle)
				.containsExactly("상시 소개");
	}

	@Test
	void 숨긴_배너는_기간_안이어도_보여_주지_않는다() {
		promotionBannerRepository.save(PromotionBannerFixture.hidden(PromotionBannerFixture.introduction("숨김", 1)));

		assertThat(promotionBannerFinder.findVisibleBanners()).isEmpty();
	}

	@Test
	void 정렬_순서가_작은_배너부터_같으면_먼저_등록한_배너부터_보여_준다() {
		promotionBannerRepository.save(PromotionBannerFixture.introduction("둘째", 2));
		promotionBannerRepository.save(PromotionBannerFixture.introduction("첫째 먼저", 1));
		promotionBannerRepository.save(PromotionBannerFixture.introduction("첫째 나중", 1));

		assertThat(promotionBannerFinder.findVisibleBanners())
				.extracting(PromotionBanner::getTitle)
				.containsExactly("첫째 먼저", "첫째 나중", "둘째");
	}
}
