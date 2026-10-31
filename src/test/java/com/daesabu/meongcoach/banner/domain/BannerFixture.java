package com.daesabu.meongcoach.banner.domain;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class BannerFixture {

	private BannerFixture() {
	}

	public static Banner benefit(String title, Instant startsAt, Instant expiresAt, int sortOrder) {
		Banner banner = base(BannerType.BENEFIT, "OO펫", title, sortOrder);
		ReflectionTestUtils.setField(banner, "brandCategory", "반려용품");
		ReflectionTestUtils.setField(banner, "benefitDescription", "전 상품 10% 할인");
		ReflectionTestUtils.setField(banner, "usageLocation", "OO펫 온라인 스토어");
		ReflectionTestUtils.setField(banner, "promotionCode", "MEONG10");
		ReflectionTestUtils.setField(banner, "startsAt", startsAt);
		ReflectionTestUtils.setField(banner, "expiresAt", expiresAt);
		return banner;
	}

	public static Banner introduction(String title, int sortOrder) {
		Banner banner = base(BannerType.INTRODUCTION, "멍프탑", title, sortOrder);
		ReflectionTestUtils.setField(banner, "brandDescription", "멍프탑의 위치와 운영 시간은 네이버 플레이스에서 확인해 주세요.");
		ReflectionTestUtils.setField(banner, "linkLabel", "네이버 플레이스");
		return banner;
	}

	public static Banner withId(Banner banner, Long id) {
		ReflectionTestUtils.setField(banner, "id", id);
		return banner;
	}

	public static Banner inactive(Banner banner) {
		ReflectionTestUtils.setField(banner, "active", false);
		return banner;
	}

	private static Banner base(BannerType type, String brandName, String title, int sortOrder) {
		Banner banner = new Banner();
		ReflectionTestUtils.setField(banner, "type", type);
		ReflectionTestUtils.setField(banner, "brandName", brandName);
		ReflectionTestUtils.setField(banner, "title", title);
		ReflectionTestUtils.setField(banner, "imageUrl", "https://images.meongcoach.com/images/partner-banners/banner.jpg");
		ReflectionTestUtils.setField(banner, "linkUrl", "https://example.com");
		ReflectionTestUtils.setField(banner, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(banner, "active", true);
		return banner;
	}
}
