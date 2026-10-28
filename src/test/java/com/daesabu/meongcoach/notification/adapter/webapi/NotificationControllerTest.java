package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.HourPreferenceRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.domain.HourPreference;
import com.daesabu.meongcoach.notification.domain.exception.InvalidDevicePlatformException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureRestDocs
class NotificationControllerTest {

	private static final String TOKENS_PATH = "/api/notifications/tokens";
	private static final String PREFERENCES_PATH = "/api/notifications/preferences";

	private static final Principal CURRENT_USER = () -> "42";

	private static final String TOKEN_REGISTER_BODY = """
			{"token": "fcm-registration-token", "platform": "IOS"}
			""";

	private static final String PREFERENCES_UPDATE_BODY = """
			{"hours": [
			  {"hour": 8, "enabled": true},
			  {"hour": 11, "enabled": false},
			  {"hour": 17, "enabled": true}
			]}
			""";

	private static final FieldDescriptor[] ERROR_FIELDS = {
			fieldWithPath("title").description("HTTP 상태 이름"),
			fieldWithPath("status").description("HTTP 상태 코드"),
			fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
			fieldWithPath("instance").description("에러가 발생한 요청 경로"),
			fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
			fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
	};

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationRegister notificationRegister;

	@MockitoBean
	private NotificationPreferenceFinder notificationPreferenceFinder;

	@Test
	void 기기_토큰을_등록하면_204를_반환한다() throws Exception {
		mockMvc.perform(put(TOKENS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_REGISTER_BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/token-register",
						requestFields(
								fieldWithPath("token").description("FCM 등록 토큰"),
								fieldWithPath("platform").description("기기 플랫폼(IOS 또는 ANDROID)")
						)
				));

		then(notificationRegister).should()
				.registerDeviceToken(42L, new DeviceTokenRegisterRequest("fcm-registration-token", "IOS"));
	}

	@Test
	void 토큰이_비어_있으면_등록은_검증에_실패한다() throws Exception {
		mockMvc.perform(put(TOKENS_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_REGISTER_BODY.replace("\"fcm-registration-token\"", "\" \"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("token"));

		then(notificationRegister).shouldHaveNoInteractions();
	}

	@Test
	void 지원하지_않는_플랫폼이면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new InvalidDevicePlatformException("WEB")).given(notificationRegister)
				.registerDeviceToken(42L, new DeviceTokenRegisterRequest("fcm-registration-token", "WEB"));

		mockMvc.perform(put(TOKENS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_REGISTER_BODY.replace("\"IOS\"", "\"WEB\"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_PLATFORM_INVALID"))
				.andDo(document("notification/token-register-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 기기_토큰을_지우면_204를_반환한다() throws Exception {
		mockMvc.perform(delete(TOKENS_PATH + "/{token}", "fcm-registration-token")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNoContent())
				.andDo(document("notification/token-delete",
						pathParameters(parameterWithName("token").description("지울 FCM 등록 토큰"))
				));

		then(notificationRegister).should().deleteDeviceToken(42L, "fcm-registration-token");
	}

	@Test
	void 알림_설정을_시각별로_응답한다() throws Exception {
		given(notificationPreferenceFinder.findHourPreferences(42L)).willReturn(List.of(
				new HourPreference(8, false),
				new HourPreference(11, false),
				new HourPreference(17, true)
		));

		mockMvc.perform(get(PREFERENCES_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hours.length()").value(3))
				.andExpect(jsonPath("$.hours[0].hour").value(8))
				.andExpect(jsonPath("$.hours[0].enabled").value(false))
				.andExpect(jsonPath("$.hours[2].hour").value(17))
				.andExpect(jsonPath("$.hours[2].enabled").value(true))
				.andDo(document("notification/preferences",
						responseFields(
								fieldWithPath("hours").description("시각별 알림 설정. 8·11·17시 순서"),
								fieldWithPath("hours[].hour").description("알림 시각(Asia/Seoul 기준 0~23)"),
								fieldWithPath("hours[].enabled").description("해당 시각 알림 수신 여부")
						)
				));
	}

	@Test
	void 알림_설정을_바꾸면_204를_반환한다() throws Exception {
		mockMvc.perform(put(PREFERENCES_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES_UPDATE_BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/preferences-update",
						requestFields(
								fieldWithPath("hours").description("시각별 알림 설정. 8·11·17시를 각각 한 번씩 보낸다"),
								fieldWithPath("hours[].hour").description("알림 시각(Asia/Seoul 기준 0~23)"),
								fieldWithPath("hours[].enabled").description("해당 시각 알림 수신 여부")
						)
				));

		then(notificationRegister).should().updatePreferences(42L, new NotificationPreferenceUpdateRequest(List.of(
				new HourPreferenceRequest(8, true),
				new HourPreferenceRequest(11, false),
				new HourPreferenceRequest(17, true)
		)));
	}

	@Test
	void 허용_시각이_빠지거나_중복되면_400과_에러_코드를_반환한다() throws Exception {
		NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(List.of(
				new HourPreferenceRequest(8, true),
				new HourPreferenceRequest(8, false),
				new HourPreferenceRequest(17, true)
		));
		willThrow(new InvalidNotificationHoursException()).given(notificationRegister).updatePreferences(42L, request);

		mockMvc.perform(put(PREFERENCES_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES_UPDATE_BODY.replace("\"hour\": 11", "\"hour\": 8")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_HOURS_INVALID"))
				.andDo(document("notification/preferences-update-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 시각_설정에_enabled가_없으면_검증에_실패한다() throws Exception {
		mockMvc.perform(put(PREFERENCES_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES_UPDATE_BODY.replace("\"enabled\": false", "\"enabled\": null")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("hours[1].enabled"));

		then(notificationRegister).shouldHaveNoInteractions();
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get(PREFERENCES_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(notificationPreferenceFinder).shouldHaveNoInteractions();
	}
}
