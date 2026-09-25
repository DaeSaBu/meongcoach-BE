package com.daesabu.meongcoach.purchase.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.purchase.application.provided.PurchaseRegister;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RevenueCatWebhookController.class)
@AutoConfigureRestDocs
// 컨트롤러 슬라이스에는 @ConfigurationPropertiesScan이 적용되지 않아 컨트롤러가 쓰는 설정을 직접 등록한다
@EnableConfigurationProperties(RevenueCatWebhookProperties.class)
class RevenueCatWebhookControllerTest {

	private static final String WEBHOOK_PATH = "/api/webhooks/revenuecat";

	// application-test.yml의 meongcoach.revenuecat.webhook-authorization 값
	private static final String WEBHOOK_AUTHORIZATION = "test-revenuecat-webhook-authorization";

	private static final String TRANSACTION_ID = "2000000912345678";
	private static final long PURCHASED_AT_MS = 1790181485867L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PurchaseRegister purchaseRegister;

	@Test
	void 평생권_구매_이벤트면_회원_ID로_구매를_등록한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhook("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isOk())
				.andDo(document("purchase/revenuecat-webhook",
						requestHeaders(
								headerWithName(HttpHeaders.AUTHORIZATION)
										.description("RevenueCat 대시보드에 설정한 웹훅 인증값 (`Bearer` 접두어 없음)")
						),
						// RevenueCat이 보내는 필드 중 구매 등록에 쓰지 않는 필드는 무시한다
						requestFields(
								fieldWithPath("api_version").ignored(),
								fieldWithPath("event.type")
										.description("이벤트 타입. `NON_RENEWING_PURCHASE`만 처리하고 나머지는 무시한다"),
								fieldWithPath("event.id").description("RevenueCat 이벤트 ID"),
								fieldWithPath("event.app_user_id")
										.description("구매자 ID. 회원 ID(숫자)가 아니면 저장하지 않는다"),
								fieldWithPath("event.transaction_id").description("스토어 거래 ID"),
								fieldWithPath("event.product_id").description("스토어 상품 ID"),
								fieldWithPath("event.store").description("구매한 스토어. `APP_STORE`, `PLAY_STORE` 등"),
								fieldWithPath("event.price_in_purchased_currency").optional()
										.description("결제 통화 기준 가격"),
								fieldWithPath("event.currency").optional().description("ISO 4217 통화 코드"),
								fieldWithPath("event.purchased_at_ms").description("구매 시각(epoch ms)"),
								fieldWithPath("event.entitlement_ids").optional()
										.description("이 구매로 부여할 entitlement 식별자 목록"),
								fieldWithPath("event.original_app_user_id").ignored(),
								fieldWithPath("event.aliases").ignored(),
								fieldWithPath("event.environment").ignored(),
								fieldWithPath("event.original_transaction_id").ignored(),
								fieldWithPath("event.price").ignored(),
								fieldWithPath("event.event_timestamp_ms").ignored()
						)
				));

		then(purchaseRegister).should().register(new PurchaseRegisterRequest(42L, TRANSACTION_ID,
				"meongcoach_all_lifetime", "APP_STORE", new BigDecimal("6.99"), "USD",
				Instant.ofEpochMilli(PURCHASED_AT_MS), Set.of("puppy", "junior", "adult", "senior")));
	}

	@Test
	void 처리하지_않는_이벤트_타입이면_구매를_등록하지_않고_200을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhook("TEST", "42")))
				.andExpect(status().isOk());

		then(purchaseRegister).shouldHaveNoInteractions();
	}

	@Test
	void 익명_사용자의_구매면_구매를_등록하지_않고_200을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhook("NON_RENEWING_PURCHASE", "$RCAnonymousID:8f3b2c1d")))
				.andExpect(status().isOk());

		then(purchaseRegister).shouldHaveNoInteractions();
	}

	@Test
	void 인증값이_틀리면_401과_에러_코드를_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, "wrong-authorization")
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhook("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PURCHASE_WEBHOOK_UNAUTHORIZED"))
				.andDo(document("purchase/revenuecat-webhook-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));

		then(purchaseRegister).shouldHaveNoInteractions();
	}

	@Test
	void 인증값이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhook("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PURCHASE_WEBHOOK_UNAUTHORIZED"));

		then(purchaseRegister).shouldHaveNoInteractions();
	}

	// sandbox 평생권 구매 이벤트에서 쓰지 않는 필드 일부를 남겨 두어 모르는 필드를 무시하는지도 함께 확인한다
	private static String webhook(String type, String appUserId) {
		return """
				{
				  "api_version": "1.0",
				  "event": {
				    "id": "CD489E0E-5D2F-4F1B-9B7A-4C3E2A1B0F9D",
				    "type": "%s",
				    "app_user_id": "%s",
				    "original_app_user_id": "%s",
				    "aliases": ["%s"],
				    "environment": "SANDBOX",
				    "transaction_id": "%s",
				    "original_transaction_id": "%s",
				    "product_id": "meongcoach_all_lifetime",
				    "store": "APP_STORE",
				    "price": 6.99,
				    "price_in_purchased_currency": 6.99,
				    "currency": "USD",
				    "purchased_at_ms": %d,
				    "entitlement_ids": ["puppy", "junior", "adult", "senior"],
				    "event_timestamp_ms": 1790181486000
				  }
				}
				""".formatted(type, appUserId, appUserId, appUserId, TRANSACTION_ID, TRANSACTION_ID, PURCHASED_AT_MS);
	}
}
