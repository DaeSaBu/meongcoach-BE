package com.daesabu.meongcoach.partner.domain;

import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

public final class PartnerBannerFixture {

	private PartnerBannerFixture() {
	}

	public static PartnerBanner benefit(String title, LocalDate startsOn, LocalDate endsOn, int sortOrder) {
		PartnerBanner banner = base(PartnerBannerKind.BENEFIT, "OO펫", title, sortOrder);
		ReflectionTestUtils.setField(banner, "partnerKind", "반려용품");
		ReflectionTestUtils.setField(banner, "benefit", "전 상품 10% 할인");
		ReflectionTestUtils.setField(banner, "place", "OO펫 온라인 스토어");
		ReflectionTestUtils.setField(banner, "validityLabel", "12월 31일까지");
		ReflectionTestUtils.setField(banner, "sharedCode", "MEONG10");
		ReflectionTestUtils.setField(banner, "disclosure", true);
		ReflectionTestUtils.setField(banner, "startsOn", startsOn);
		ReflectionTestUtils.setField(banner, "endsOn", endsOn);
		return banner;
	}

	public static PartnerBanner introduction(String title, int sortOrder) {
		PartnerBanner banner = base(PartnerBannerKind.INTRODUCTION, "멍프탑", title, sortOrder);
		ReflectionTestUtils.setField(banner, "description", "멍프탑의 위치와 운영 시간은 네이버 플레이스에서 확인해 주세요.");
		ReflectionTestUtils.setField(banner, "linkLabel", "네이버 플레이스");
		ReflectionTestUtils.setField(banner, "disclosure", true);
		return banner;
	}

	public static PartnerBanner withId(PartnerBanner banner, Long id) {
		ReflectionTestUtils.setField(banner, "id", id);
		return banner;
	}

	public static PartnerBanner hidden(PartnerBanner banner) {
		ReflectionTestUtils.setField(banner, "active", false);
		return banner;
	}

	private static PartnerBanner base(PartnerBannerKind kind, String partnerName, String title, int sortOrder) {
		PartnerBanner banner = new PartnerBanner();
		ReflectionTestUtils.setField(banner, "kind", kind);
		ReflectionTestUtils.setField(banner, "partnerName", partnerName);
		ReflectionTestUtils.setField(banner, "title", title);
		ReflectionTestUtils.setField(banner, "imageUrl", "https://images.meongcoach.com/images/partner-banners/banner.jpg");
		ReflectionTestUtils.setField(banner, "imageBackground", "#ffffff");
		ReflectionTestUtils.setField(banner, "linkUrl", "https://example.com");
		ReflectionTestUtils.setField(banner, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(banner, "active", true);
		return banner;
	}
}
