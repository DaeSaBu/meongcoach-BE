package com.daesabu.meongcoach.coupon.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.coupon.application.provided.CouponRedemptionRegister;
import com.daesabu.meongcoach.coupon.application.provided.dto.CouponRedemptionRegisterRequest;
import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.CouponFixture;
import com.daesabu.meongcoach.coupon.domain.CouponRedemption;
import com.daesabu.meongcoach.coupon.domain.exception.CouponAlreadyRedeemedException;
import com.daesabu.meongcoach.coupon.domain.exception.CouponExpiredException;
import com.daesabu.meongcoach.coupon.domain.exception.CouponNotFoundException;
import java.security.Principal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponController.class)
@AutoConfigureRestDocs
class CouponControllerTest {

	private static final String REDEMPTIONS_PATH = "/api/coupons/redemptions";

	private static final Principal CURRENT_USER = () -> "42";

	private static final String REQUEST_BODY = """
			{"code": "KINTEX"}
			""";

	private static final CouponRedemptionRegisterRequest REQUEST = new CouponRedemptionRegisterRequest("KINTEX");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CouponRedemptionRegister couponRedemptionRegister;

	@Test
	void 쿠폰을_사용하고_지급된_AI_분석_횟수를_201로_반환한다() throws Exception {
		given(couponRedemptionRegister.register(42L, REQUEST)).willReturn(redemption());

		mockMvc.perform(post(REDEMPTIONS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.aiTrialCount").value(10))
				.andDo(document("coupon/redemption-register",
						requestFields(
								fieldWithPath("code").description(
										"필수 입력. 쿠폰 코드. 최대 50자이며 앞뒤 공백과 대소문자는 구분하지 않는다")
						),
						responseFields(
								fieldWithPath("aiTrialCount").description(
										"이번 사용으로 늘어난 AI 행동 분석 부여 횟수. 남은 횟수는 AI 리포트 체험 현황 조회로 확인한다")
						)
				));
	}

	@Test
	void 요청_코드의_앞뒤_공백을_지워_사용을_위임한다() throws Exception {
		given(couponRedemptionRegister.register(42L, REQUEST)).willReturn(redemption());

		mockMvc.perform(post(REDEMPTIONS_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code": "  KINTEX  "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.aiTrialCount").value(10));
	}

	@Test
	void 이미_사용한_쿠폰이면_409와_에러_코드를_반환한다() throws Exception {
		given(couponRedemptionRegister.register(42L, REQUEST)).willThrow(new CouponAlreadyRedeemedException());

		mockMvc.perform(post(REDEMPTIONS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("COUPON_ALREADY_REDEEMED"))
				.andDo(document("coupon/redemption-register-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 없는_코드면_404와_에러_코드를_반환한다() throws Exception {
		given(couponRedemptionRegister.register(42L, REQUEST)).willThrow(new CouponNotFoundException());

		mockMvc.perform(post(REDEMPTIONS_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
	}

	@Test
	void 입력_기한이_지난_쿠폰이면_400과_에러_코드를_반환한다() throws Exception {
		given(couponRedemptionRegister.register(42L, REQUEST)).willThrow(new CouponExpiredException());

		mockMvc.perform(post(REDEMPTIONS_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COUPON_EXPIRED"));
	}

	@Test
	void 코드가_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(post(REDEMPTIONS_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code": " "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("code"));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post(REDEMPTIONS_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private CouponRedemption redemption() {
		Coupon coupon = CouponFixture.withId(CouponFixture.unlimited("KINTEX", 10), 3L);
		return coupon.redeem(42L, Instant.now());
	}
}
