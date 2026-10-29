package com.daesabu.meongcoach.banner.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.banner.application.provided.BannerFinder;
import com.daesabu.meongcoach.banner.domain.BannerFixture;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BannerController.class)
@AutoConfigureRestDocs
class BannerControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BannerFinder bannerFinder;

	@Test
	void 노출_중인_제휴_배너를_순서대로_반환한다() throws Exception {
		given(bannerFinder.findVisibleBanners()).willReturn(List.of(
				BannerFixture.withId(BannerFixture.benefit("멍코치 회원 전 상품 10% 할인",
						Instant.parse("2026-10-31T15:00:00Z"), Instant.parse("2026-12-31T15:00:00Z"), 1), 3L),
				BannerFixture.withId(BannerFixture.introduction("멍프탑을 만나보세요", 2), 5L)));

		mockMvc.perform(get("/api/banners")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.banners[0].disclosure").doesNotExist())
				.andExpect(jsonPath("$.banners[0].validityLabel").doesNotExist())
				.andExpect(jsonPath("$.banners[0].imageBackground").doesNotExist())
				.andExpect(jsonPath("$.banners[0].id").value(3))
				.andExpect(jsonPath("$.banners[0].type").value("BENEFIT"))
				.andExpect(jsonPath("$.banners[0].sharedCode").value("MEONG10"))
				.andExpect(jsonPath("$.banners[0].startsAt").value("2026-10-31T15:00:00Z"))
				.andExpect(jsonPath("$.banners[0].expiresAt").value("2026-12-31T15:00:00Z"))
				.andExpect(jsonPath("$.banners[1].id").value(5))
				.andExpect(jsonPath("$.banners[1].type").value("INTRODUCTION"))
				.andExpect(jsonPath("$.banners[1].linkLabel").value("네이버 플레이스"))
				.andExpect(jsonPath("$.banners[1].expiresAt").isEmpty())
				.andDo(document("banner/list",
						responseFields(
								fieldWithPath("banners").description("현재 시각에 노출할 배너. 정렬 순서대로이고 없으면 빈 배열"),
								fieldWithPath("banners[].id").description("배너 ID. 분석 이벤트와 utm_campaign에 쓴다"),
								fieldWithPath("banners[].type").description("배너 종류. BENEFIT(기간이 있는 혜택), INTRODUCTION(업체 소개)"),
								fieldWithPath("banners[].brandName").description("업체 이름"),
								fieldWithPath("banners[].brandCategory").optional().description("업종. 없으면 null"),
								fieldWithPath("banners[].title").description("배너 제목"),
								fieldWithPath("banners[].imageUrl").description("배너 이미지 공개 URL"),
								fieldWithPath("banners[].linkUrl").description("업체 페이지 URL. 앱이 utm 파라미터를 붙여 연다"),
								fieldWithPath("banners[].linkLabel").optional().description("소개 배너의 이동 버튼 이름. 혜택 배너는 null일 수 있다"),
								fieldWithPath("banners[].description").optional().description("소개 배너의 설명. 혜택 배너는 null일 수 있다"),
								fieldWithPath("banners[].benefit").optional().description("혜택 내용. 소개 배너는 null"),
								fieldWithPath("banners[].place").optional().description("혜택 사용처. 소개 배너는 null"),
								fieldWithPath("banners[].sharedCode").optional().description("모든 회원이 같이 쓰는 할인 코드. 없으면 null"),
								fieldWithPath("banners[].startsAt").optional().description("노출 시작 시각(UTC, 포함). 없으면 null"),
								fieldWithPath("banners[].expiresAt").optional().description("만료 시각(UTC, 미포함). 없으면 종료 제한 없음")
						)
				));
	}
}
