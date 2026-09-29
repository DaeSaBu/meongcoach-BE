package com.daesabu.meongcoach.purchase.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.purchase.application.provided.PurchaseSynchronizer;
import com.daesabu.meongcoach.purchase.domain.exception.StorePurchaseUnavailableException;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PurchaseController.class)
@AutoConfigureRestDocs
class PurchaseControllerTest {

	private static final String SYNCHRONIZATION_PATH = "/api/purchases/synchronization";

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PurchaseSynchronizer purchaseSynchronizer;

	@Test
	void 인증한_회원의_스토어_구매를_동기화하고_204를_반환한다() throws Exception {
		mockMvc.perform(post(SYNCHRONIZATION_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNoContent())
				.andDo(document("purchase/synchronize"));

		then(purchaseSynchronizer).should().synchronize(42L);
	}

	@Test
	void RevenueCat에서_구매를_조회하지_못하면_502와_에러_코드를_반환한다() throws Exception {
		willThrow(new StorePurchaseUnavailableException()).given(purchaseSynchronizer).synchronize(42L);

		mockMvc.perform(post(SYNCHRONIZATION_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.code").value("PURCHASE_STORE_UNAVAILABLE"))
				.andDo(document("purchase/synchronize-error",
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
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post(SYNCHRONIZATION_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
