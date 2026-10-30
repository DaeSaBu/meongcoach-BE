package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationDeleteRequest;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
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
	private static final String TOKENS_PATH = "/internal/notifications/installations";
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

	private static final String TOKEN_DELETE_BODY = """
			{"fids": ["expired-fid-1", "expired-fid-2"]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationTargetFinder notificationTargetFinder;

	@MockitoBean
	private NotificationRegister notificationRegister;


	@Test
	void 시각의_발송_대상을_FID_한_행씩_배열로_응답한다() throws Exception {
		given(notificationTargetFinder.findTargets(17)).willReturn(List.of(
				new NotificationTargetResult(42L, "fid-ios", DevicePlatform.IOS),
				new NotificationTargetResult(42L, "fid-android", DevicePlatform.ANDROID),
				new NotificationTargetResult(43L, "fid-43", DevicePlatform.IOS)
		));

		mockMvc.perform(get(TARGETS_PATH)
						.queryParam("hour", "17")
						.header(INTERNAL_SECRET_HEADER, "internal-secret"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].userId").value(42))
				.andExpect(jsonPath("$[0].fid").value("fid-ios"))
				.andExpect(jsonPath("$[0].platform").value("IOS"))
				.andExpect(jsonPath("$[1].userId").value(42))
				.andExpect(jsonPath("$[1].platform").value("ANDROID"))
				.andExpect(jsonPath("$[2].userId").value(43))
				.andDo(document("notification/internal-targets",
						queryParameters(parameterWithName("hour").description("알림 시각(Asia/Seoul 기준 0~23)")),
						responseFields(
								fieldWithPath("[].userId").description("회원 ID. 회원 ID 오름차순, 같은 회원 안에서는 FID 등록 순"),
								fieldWithPath("[].fid").description("Firebase Installation ID(FID)"),
								fieldWithPath("[].platform").description("기기 플랫폼(IOS·ANDROID)")
						)
				));
	}

	@Test
	void 발송_대상이_없으면_빈_배열을_응답한다() throws Exception {
		given(notificationTargetFinder.findTargets(3)).willReturn(List.of());

		mockMvc.perform(get(TARGETS_PATH)
						.queryParam("hour", "3")
						.header(INTERNAL_SECRET_HEADER, "internal-secret"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void 시각이_0에서_23_밖이면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new InvalidNotificationHourException(24)).given(notificationTargetFinder).findTargets(24);

		mockMvc.perform(get(TARGETS_PATH)
						.queryParam("hour", "24")
						.header(INTERNAL_SECRET_HEADER, "internal-secret"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_HOUR_INVALID"))
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
	}

	@Test
	void 시각이_숫자가_아니면_400을_반환한다() throws Exception {
		mockMvc.perform(get(TARGETS_PATH)
						.queryParam("hour", "evening")
						.header(INTERNAL_SECRET_HEADER, "internal-secret"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));

		then(notificationTargetFinder).shouldHaveNoInteractions();
	}

	@Test
	void 무효_FID_목록을_지우면_204를_반환한다() throws Exception {
		mockMvc.perform(delete(TOKENS_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_DELETE_BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/internal-fid-delete",
						requestFields(fieldWithPath("fids").description("FCM이 무효라고 답한 등록 FID 목록"))
				));

		then(notificationRegister).should()
				.deleteDeviceInstallations(new DeviceInstallationDeleteRequest(List.of("expired-fid-1", "expired-fid-2")));
	}

	@Test
	void 빈_문자열_FID이_섞이면_검증에_실패한다() throws Exception {
		mockMvc.perform(delete(TOKENS_PATH)
						.header(INTERNAL_SECRET_HEADER, "internal-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_DELETE_BODY.replace("\"expired-fid-2\"", "\" \"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fids[1]"));

		then(notificationRegister).shouldHaveNoInteractions();
	}




}
