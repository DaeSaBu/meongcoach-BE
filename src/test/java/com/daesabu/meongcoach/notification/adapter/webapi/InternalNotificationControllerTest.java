package com.daesabu.meongcoach.notification.adapter.webapi;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.DeviceTokenResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenDeleteRequest;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
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

	private static final String TARGETS_PATH = "/internal/notifications/targets";
	private static final String TOKENS_PATH = "/internal/notifications/tokens";
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

	private static final String TOKEN_DELETE_BODY = """
			{"tokens": ["expired-token-1", "expired-token-2"]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationTargetFinder notificationTargetFinder;

	@MockitoBean
	private NotificationRegister notificationRegister;

	@Test
	void 시간대의_발송_대상을_회원별_토큰_목록으로_응답한다() throws Exception {
		given(notificationTargetFinder.findTargets(NotificationSlot.EVENING)).willReturn(List.of(
				new NotificationTargetResult(42L, List.of(
						new DeviceTokenResult("token-ios", DevicePlatform.IOS),
						new DeviceTokenResult("token-android", DevicePlatform.ANDROID)
				))
		));

		mockMvc.perform(get(TARGETS_PATH)
						.queryParam("slot", "EVENING")
						.header(INTERNAL_SECRET_HEADER, "internal-secret"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slot").value("EVENING"))
				.andExpect(jsonPath("$.targets[0].userId").value(42))
				.andExpect(jsonPath("$.targets[0].tokens.length()").value(2))
				.andExpect(jsonPath("$.targets[0].tokens[0].token").value("token-ios"))
				.andExpect(jsonPath("$.targets[0].tokens[0].platform").value("IOS"))
				.andDo(document("notification/internal-targets",
						queryParameters(parameterWithName("slot").description("알림 시간대(MORNING·NOON·EVENING)")),
						responseFields(
								fieldWithPath("slot").description("요청한 알림 시간대"),
								fieldWithPath("targets").description("발송 대상 회원 목록. 토큰이 없는 회원은 빠진다"),
								fieldWithPath("targets[].userId").description("회원 ID"),
								fieldWithPath("targets[].tokens").description("회원의 기기 토큰 목록"),
								fieldWithPath("targets[].tokens[].token").description("FCM 등록 토큰"),
								fieldWithPath("targets[].tokens[].platform").description("기기 플랫폼(IOS·ANDROID)")
						)
				));
	}

	@Test
	void 알_수_없는_시간대면_400과_에러_코드를_반환한다() throws Exception {
		mockMvc.perform(get(TARGETS_PATH)
						.queryParam("slot", "MIDNIGHT")
						.header(INTERNAL_SECRET_HEADER, "internal-secret"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_SLOT_INVALID"))
				.andDo(document("notification/internal-targets-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));

		then(notificationTargetFinder).shouldHaveNoInteractions();
	}

	@Test
	void 무효_토큰_목록을_지우면_204를_반환한다() throws Exception {
		mockMvc.perform(delete(TOKENS_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_DELETE_BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/internal-token-delete",
						requestFields(fieldWithPath("tokens").description("FCM이 무효라고 답한 등록 토큰 목록"))
				));

		then(notificationRegister).should()
				.deleteDeviceTokens(new DeviceTokenDeleteRequest(List.of("expired-token-1", "expired-token-2")));
	}

	@Test
	void 빈_문자열_토큰이_섞이면_검증에_실패한다() throws Exception {
		mockMvc.perform(delete(TOKENS_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_DELETE_BODY.replace("\"expired-token-2\"", "\" \"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("tokens[1]"));

		then(notificationRegister).shouldHaveNoInteractions();
	}
}
