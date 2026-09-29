package com.daesabu.meongcoach.entitlement.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RevenueCatWebhookController.class)
@AutoConfigureRestDocs
class RevenueCatWebhookControllerTest {

	private static final String WEBHOOK_PATH = "/api/webhooks/revenuecat";

	// application-test.yml의 meongcoach.revenuecat.webhook-authorization 값
	private static final String WEBHOOK_AUTHORIZATION = "test-revenuecat-webhook-authorization";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EntitlementSynchronizer entitlementSynchronizer;

	@Test
	void 구매_이벤트면_이벤트의_회원을_동기화한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isOk())
				.andDo(document("entitlement/revenuecat-webhook",
						requestHeaders(
								headerWithName(HttpHeaders.AUTHORIZATION)
										.description("RevenueCat 대시보드에 설정한 웹훅 인증값 (`Bearer` 접두어 없음)")
						),
						// 이벤트 내용은 해석하지 않고 동기화할 회원만 고른다. 나머지 필드는 받지만 쓰지 않는다
						requestFields(
								fieldWithPath("event.type")
										.description("이벤트 타입. 종류와 상관없이 관련 회원을 동기화한다"),
								fieldWithPath("event.app_user_id").optional().type(JsonFieldType.STRING)
										.description("이벤트의 회원 ID. 회원 ID(숫자)가 아니면 건너뛴다"),
								fieldWithPath("event.transferred_from").optional().type(JsonFieldType.ARRAY)
										.description("`TRANSFER`에서 이용권을 잃는 회원 ID 목록"),
								fieldWithPath("event.transferred_to").optional().type(JsonFieldType.ARRAY)
										.description("`TRANSFER`에서 이용권을 받는 회원 ID 목록"),
								fieldWithPath("api_version").ignored(),
								fieldWithPath("event.id").ignored(),
								fieldWithPath("event.original_app_user_id").ignored(),
								fieldWithPath("event.aliases").ignored(),
								fieldWithPath("event.environment").ignored(),
								fieldWithPath("event.transaction_id").ignored(),
								fieldWithPath("event.product_id").ignored(),
								fieldWithPath("event.store").ignored(),
								fieldWithPath("event.purchased_at_ms").ignored(),
								fieldWithPath("event.entitlement_ids").ignored(),
								fieldWithPath("event.event_timestamp_ms").ignored()
						)
				));

		then(entitlementSynchronizer).should().synchronize(42L);
	}

	@Test
	void 환불_이벤트도_이벤트의_회원을_동기화한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("CANCELLATION", "42")))
				.andExpect(status().isOk());

		then(entitlementSynchronizer).should().synchronize(42L);
	}

	// 이전된 회원은 이용권을 잃고 받는 회원은 얻는다. 웹훅은 받는 회원 쪽으로만 한 번 오므로 양쪽을 모두 맞춘다
	@Test
	void 계정_이전_이벤트면_이용권을_잃는_회원과_받는_회원을_모두_동기화한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(transferEvent("41", "42")))
				.andExpect(status().isOk());

		then(entitlementSynchronizer).should().synchronize(41L);
		then(entitlementSynchronizer).should().synchronize(42L);
	}

	@Test
	void 익명_사용자의_이벤트면_동기화하지_않고_200을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("NON_RENEWING_PURCHASE", "$RCAnonymousID:8f3b2c1d")))
				.andExpect(status().isOk());

		then(entitlementSynchronizer).shouldHaveNoInteractions();
	}

	// 200이 아니면 RevenueCat이 재전송한다. 회원 매핑을 고친 뒤 대시보드의 Retry로 다시 받아 복구한다
	@Test
	void 없는_회원의_이벤트면_404와_에러_코드를_반환한다() throws Exception {
		willThrow(new UserNotFoundException(42L)).given(entitlementSynchronizer).synchronize(any());

		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andDo(document("entitlement/revenuecat-webhook-user-not-found-error",
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

	// 이용권이 없는 것과 조회하지 못한 것을 구분해 RevenueCat이 재전송하게 한다
	@Test
	void RevenueCat에서_이용권을_조회하지_못하면_502를_반환한다() throws Exception {
		willThrow(new EntitlementProviderUnavailableException()).given(entitlementSynchronizer).synchronize(any());

		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("ENTITLEMENT_PROVIDER_UNAVAILABLE"));
	}

	@Test
	void 인증값이_틀리면_401과_에러_코드를_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, "wrong-authorization")
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ENTITLEMENT_WEBHOOK_UNAUTHORIZED"))
				.andDo(document("entitlement/revenuecat-webhook-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));

		then(entitlementSynchronizer).shouldHaveNoInteractions();
	}

	@Test
	void 인증값이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(purchaseEvent("NON_RENEWING_PURCHASE", "42")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ENTITLEMENT_WEBHOOK_UNAUTHORIZED"));

		then(entitlementSynchronizer).shouldHaveNoInteractions();
	}

	@Test
	void 인증값이_틀리면_본문이_잘못돼도_401을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.header(HttpHeaders.AUTHORIZATION, "wrong-authorization")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ENTITLEMENT_WEBHOOK_UNAUTHORIZED"));

		then(entitlementSynchronizer).shouldHaveNoInteractions();
	}

	// sandbox 평생권 이벤트에서 쓰지 않는 필드 일부를 남겨 두어 모르는 필드를 무시하는지도 함께 확인한다
	private static String purchaseEvent(String type, String appUserId) {
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
				    "transaction_id": "2000000912345678",
				    "product_id": "meongcoach_all_lifetime",
				    "store": "APP_STORE",
				    "purchased_at_ms": 1790181485867,
				    "entitlement_ids": ["puppy", "junior", "adult", "senior"],
				    "event_timestamp_ms": 1790181486000
				  }
				}
				""".formatted(type, appUserId, appUserId, appUserId);
	}

	// TRANSFER 이벤트에는 app_user_id가 없고, 옮겨 간 양쪽 회원이 목록으로 온다
	private static String transferEvent(String transferredFrom, String transferredTo) {
		return """
				{
				  "api_version": "1.0",
				  "event": {
				    "id": "A1B2C3D4-5D2F-4F1B-9B7A-4C3E2A1B0F9D",
				    "type": "TRANSFER",
				    "environment": "SANDBOX",
				    "store": "APP_STORE",
				    "transferred_from": ["%s"],
				    "transferred_to": ["%s"],
				    "event_timestamp_ms": 1790181486000
				  }
				}
				""".formatted(transferredFrom, transferredTo);
	}
}
