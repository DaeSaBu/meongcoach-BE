package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalNotificationController.class)
@AutoConfigureRestDocs
class InternalNotificationControllerTest {

	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationDispatcher dispatcher;

	@Test
	void 예약_시각과_hour로_발송하고_결과를_응답한다() throws Exception {
		var request = new NotificationDispatchRequest(Instant.parse("2026-10-31T08:00:00Z"), 17);
		given(dispatcher.dispatch(request)).willReturn(new NotificationDispatchResult(2, false));

		mockMvc.perform(post("/internal/notifications/dispatch")
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scheduledTime\":\"2026-10-31T08:00:00Z\",\"hour\":17}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fids").value(2))
				.andExpect(jsonPath("$.invalidated").doesNotExist())
				.andExpect(jsonPath("$.failed").doesNotExist())
				.andExpect(jsonPath("$.duplicate").value(false))
				.andDo(document("notification/internal-dispatch",
						requestFields(fieldWithPath("scheduledTime").description("원래 예약 시각(ISO 8601 UTC)"),
								fieldWithPath("hour").description("서울 기준 발송 시각(0~23)")),
						responseFields(fieldWithPath("fids").description("발송 대상 FID 수"),
								fieldWithPath("duplicate").description("진행 중이거나 완료된 예약 시각 여부"))));
	}

	@Test
	void 예약_시각이_빠지면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/internal/notifications/dispatch")
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"hour\":17}"))
				.andExpect(status().isBadRequest());
		then(dispatcher).shouldHaveNoInteractions();
	}

	@Test
	void 발송_실패를_502로_응답한다() throws Exception {
		var request = new NotificationDispatchRequest(Instant.parse("2026-10-31T08:00:00Z"), 17);
		given(dispatcher.dispatch(request)).willThrow(new NotificationDeliveryException(new IllegalStateException("FCM unavailable")));
		mockMvc.perform(post("/internal/notifications/dispatch")
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scheduledTime\":\"2026-10-31T08:00:00Z\",\"hour\":17}"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_DELIVERY_FAILED"))
				.andDo(document("notification/internal-dispatch-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("에러 코드"),
								fieldWithPath("timestamp").description("발생 시각"))));
	}
}
