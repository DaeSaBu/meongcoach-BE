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

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.time.LocalDate;
import java.util.List;
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
	private static final String SCHEDULED_PATH = "/internal/notifications/scheduled-dispatches";
	private static final String SCHEDULED_BODY = "{\"date\":\"2026-10-31\",\"hour\":17,\"minute\":0}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ScheduledNotificationDispatcher scheduledDispatcher;

	@MockitoBean
	private EventNotificationDispatcher eventDispatcher;

	private final ScheduledNotificationDispatchRequest scheduledRequest =
			new ScheduledNotificationDispatchRequest(LocalDate.of(2026, 10, 31), 17, 0);

	@Test
	void 날짜와_시와_분으로_정기_알림을_발송하고_결과를_응답한다() throws Exception {
		given(scheduledDispatcher.dispatch(scheduledRequest)).willReturn(new ScheduledNotificationDispatchResult(2, false));

		mockMvc.perform(post(SCHEDULED_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(SCHEDULED_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fids").value(2))
				.andExpect(jsonPath("$.duplicate").value(false))
				.andDo(document("notification/internal-scheduled-dispatch",
						requestFields(fieldWithPath("date").description("서울 기준 예약 날짜"),
								fieldWithPath("hour").description("서울 기준 예약 시(0~23)"),
								fieldWithPath("minute").description("예약 분(0~59)")),
						responseFields(fieldWithPath("fids").description("발송 대상 FID 수"),
								fieldWithPath("duplicate").description("이미 완료된 예약 시각 여부"))));
	}

	@Test
	void 날짜가_빠지면_400을_반환한다() throws Exception {
		mockMvc.perform(post(SCHEDULED_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"hour\":17,\"minute\":0}"))
				.andExpect(status().isBadRequest());
		then(scheduledDispatcher).shouldHaveNoInteractions();
	}

	@Test
	void 발송_실패를_502로_응답한다() throws Exception {
		given(scheduledDispatcher.dispatch(scheduledRequest)).willThrow(new NotificationDeliveryException(new IllegalStateException("FCM unavailable")));
		mockMvc.perform(post(SCHEDULED_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(SCHEDULED_BODY))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_DELIVERY_FAILED"))
				.andDo(document("notification/internal-scheduled-dispatch-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("에러 코드"),
								fieldWithPath("timestamp").description("발생 시각"))));
	}

	@Test
	void 이벤트_알림을_발송하고_결과를_응답한다() throws Exception {
		var request = new EventNotificationDispatchRequest("OPERATOR_MESSAGE",
				"USERS", List.of(1L, 2L), "점검 안내", "오늘 밤 점검합니다.", "meongcoach://home");
		given(eventDispatcher.dispatch(request)).willReturn(new EventNotificationDispatchResult(2));

		mockMvc.perform(post("/internal/notifications/event-dispatches")
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"OPERATOR_MESSAGE","targetKind":"USERS","userIds":[1,2],
								"title":"점검 안내","body":"오늘 밤 점검합니다.","deepLink":"meongcoach://home"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fids").value(2))
				.andDo(document("notification/internal-event-dispatch",
						requestFields(fieldWithPath("type").description("알림 종류. AI_ANALYSIS·OPERATOR_MESSAGE·COMMENT·SUPPORT_REPLY"),
								fieldWithPath("targetKind").description("대상 종류. ALL·USERS"),
								fieldWithPath("userIds").description("USERS일 때 회원 ID 목록(1~1,000명)"),
								fieldWithPath("title").description("제목(100자 이하)"),
								fieldWithPath("body").description("본문(500자 이하)"),
								fieldWithPath("deepLink").description("선택. meongcoach:// 로 시작하는 딥링크")),
						responseFields(fieldWithPath("fids").description("발송 대상 FID 수"))));
	}

	@Test
	void 제목이_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/internal/notifications/event-dispatches")
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"COMMENT\",\"targetKind\":\"ALL\",\"title\":\"\",\"body\":\"본문\"}"))
				.andExpect(status().isBadRequest());
		then(eventDispatcher).shouldHaveNoInteractions();
	}
}
