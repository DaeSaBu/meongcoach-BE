package com.daesabu.meongcoach.partner.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.partner.application.provided.PartnerBannerFinder;
import com.daesabu.meongcoach.partner.domain.PartnerBannerFixture;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PartnerBannerController.class)
@AutoConfigureRestDocs
class PartnerBannerControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PartnerBannerFinder partnerBannerFinder;

	@Test
	void 노출_중인_제휴_배너를_순서대로_반환한다() throws Exception {
		given(partnerBannerFinder.findVisibleBanners()).willReturn(List.of(
				PartnerBannerFixture.withId(PartnerBannerFixture.benefit("멍코치 회원 전 상품 10% 할인",
						LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), 1), 3L),
				PartnerBannerFixture.withId(PartnerBannerFixture.introduction("멍프탑을 만나보세요", 2), 5L)));

		mockMvc.perform(get("/api/partners/banners")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.banners[0].id").value(3))
				.andExpect(jsonPath("$.banners[0].kind").value("BENEFIT"))
				.andExpect(jsonPath("$.banners[0].sharedCode").value("MEONG10"))
				.andExpect(jsonPath("$.banners[0].startsOn").value("2026-11-01"))
				.andExpect(jsonPath("$.banners[0].endsOn").value("2026-12-31"))
				.andExpect(jsonPath("$.banners[1].id").value(5))
				.andExpect(jsonPath("$.banners[1].kind").value("INTRODUCTION"))
				.andExpect(jsonPath("$.banners[1].linkLabel").value("네이버 플레이스"))
				.andExpect(jsonPath("$.banners[1].endsOn").isEmpty())
				.andDo(document("partner/banner-list",
						responseFields(
								fieldWithPath("banners").description("오늘(한국 날짜) 노출할 배너. 정렬 순서대로이고 없으면 빈 배열"),
								fieldWithPath("banners[].id").description("배너 ID. 분석 이벤트와 utm_campaign에 쓴다"),
								fieldWithPath("banners[].kind").description("배너 종류. BENEFIT(기간이 있는 혜택), INTRODUCTION(업체 소개)"),
								fieldWithPath("banners[].partnerName").description("업체 이름"),
								fieldWithPath("banners[].partnerKind").optional().description("업종. 없으면 null"),
								fieldWithPath("banners[].title").description("배너 제목"),
								fieldWithPath("banners[].imageUrl").description("배너 이미지 공개 URL"),
								fieldWithPath("banners[].imageBackground").description("이미지 뒤에 칠할 배경색(#rrggbb)"),
								fieldWithPath("banners[].linkUrl").description("업체 페이지 URL. 앱이 utm 파라미터를 붙여 연다"),
								fieldWithPath("banners[].linkLabel").optional().description("소개 배너의 이동 버튼 이름. 혜택 배너는 null일 수 있다"),
								fieldWithPath("banners[].description").optional().description("소개 배너의 설명. 혜택 배너는 null일 수 있다"),
								fieldWithPath("banners[].benefit").optional().description("혜택 내용. 소개 배너는 null"),
								fieldWithPath("banners[].place").optional().description("혜택 사용처. 소개 배너는 null"),
								fieldWithPath("banners[].validityLabel").optional().description("사용 기한 문구. 소개 배너는 null"),
								fieldWithPath("banners[].sharedCode").optional().description("모든 회원이 같이 쓰는 할인 코드. 없으면 null"),
								fieldWithPath("banners[].disclosure").description("제휴 표시 여부"),
								fieldWithPath("banners[].startsOn").optional().description("노출 시작일(한국 날짜, 포함). 없으면 null"),
								fieldWithPath("banners[].endsOn").optional().description("노출 종료일(한국 날짜, 포함). 없으면 null이고 상시 노출")
						)
				));
	}
}
