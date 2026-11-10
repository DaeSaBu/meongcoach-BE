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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationEventSettingUpdateRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationScheduleUpdateRequest;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.exception.InvalidDevicePlatformException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationEventTypeException;
import com.daesabu.meongcoach.notification.domain.exception.NotAllowedNotificationHourException;
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

	private static final String TOKENS_PATH = "/api/notifications/installations";
	private static final String SCHEDULES_PATH = "/api/notifications/schedules";

	private static final Principal CURRENT_USER = () -> "42";

	private static final String TOKEN_REGISTER_BODY = """
			{"fid": "fcm-registration-fid", "platform": "IOS"}
			""";

	private static final String SCHEDULE_UPDATE_BODY = """
			{"hour": 17, "minute": 0, "enabled": true}
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
	private NotificationScheduleFinder notificationScheduleFinder;

	@MockitoBean
	private NotificationEventSettingFinder notificationEventSettingFinder;

	@Test
	void 기기_FID을_등록하면_204를_반환한다() throws Exception {
		mockMvc.perform(put(TOKENS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_REGISTER_BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/fid-register",
						requestFields(
								fieldWithPath("fid").description("Firebase Installation ID(FID)"),
								fieldWithPath("platform").description("기기 플랫폼(IOS 또는 ANDROID)")
						)
				));

		then(notificationRegister).should()
				.registerDeviceInstallation(42L, new DeviceInstallationRegisterRequest("fcm-registration-fid", "IOS"));
	}

	@Test
	void FID이_비어_있으면_등록은_검증에_실패한다() throws Exception {
		mockMvc.perform(put(TOKENS_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_REGISTER_BODY.replace("\"fcm-registration-fid\"", "\" \"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fid"));

		then(notificationRegister).shouldHaveNoInteractions();
	}

	@Test
	void 지원하지_않는_플랫폼이면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new InvalidDevicePlatformException("WEB")).given(notificationRegister)
				.registerDeviceInstallation(42L, new DeviceInstallationRegisterRequest("fcm-registration-fid", "WEB"));

		mockMvc.perform(put(TOKENS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content(TOKEN_REGISTER_BODY.replace("\"IOS\"", "\"WEB\"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_PLATFORM_INVALID"))
				.andDo(document("notification/fid-register-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 기기_FID을_지우면_204를_반환한다() throws Exception {
		mockMvc.perform(delete(TOKENS_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fid\": \"fcm-registration-fid\"}"))
				.andExpect(status().isNoContent())
				.andDo(document("notification/fid-delete",
						requestFields(fieldWithPath("fid").description("지울 Firebase Installation ID(FID). URL이 아니라 본문으로 보내 접근 로그에 남지 않게 한다"))
				));

		then(notificationRegister).should().deleteDeviceInstallation(42L, new DeviceInstallationUnregisterRequest("fcm-registration-fid"));
	}

	@Test
	void 알림_설정을_시각별로_응답한다() throws Exception {
		given(notificationScheduleFinder.findSchedules(42L)).willReturn(List.of(
				new NotificationScheduleResult(8, 0, false),
				new NotificationScheduleResult(11, 0, false),
				new NotificationScheduleResult(17, 0, true)
		));

		mockMvc.perform(get(SCHEDULES_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schedules.length()").value(3))
				.andExpect(jsonPath("$.schedules[0].hour").value(8))
				.andExpect(jsonPath("$.schedules[0].minute").value(0))
				.andExpect(jsonPath("$.schedules[0].enabled").value(false))
				.andExpect(jsonPath("$.schedules[2].hour").value(17))
				.andExpect(jsonPath("$.schedules[2].enabled").value(true))
				.andDo(document("notification/schedules",
						responseFields(
								fieldWithPath("schedules").description("시각별 알림 설정. 시·분 오름차순"),
								fieldWithPath("schedules[].hour").description("알림 시(Asia/Seoul 기준 0~23)"),
								fieldWithPath("schedules[].minute").description("알림 분(0~59)"),
								fieldWithPath("schedules[].enabled").description("해당 시각 알림 수신 여부")
						)
				));
	}

	@Test
	void 시각별_알림_설정을_바꾸면_204를_반환한다() throws Exception {
		mockMvc.perform(put(SCHEDULES_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content(SCHEDULE_UPDATE_BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/schedule-update",
						requestFields(fieldWithPath("hour").description("알림 시(Asia/Seoul 기준)"),
								fieldWithPath("minute").description("알림 분. 지금은 8:00·11:00·17:00만 허용"),
								fieldWithPath("enabled").description("해당 시각 알림 수신 여부"))
				));

		then(notificationRegister).should().updateSchedule(42L, new NotificationScheduleUpdateRequest(17, 0, true));
	}

	@Test
	void 허용하지_않는_시각이면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new NotAllowedNotificationHourException(17, 0)).given(notificationRegister)
				.updateSchedule(42L, new NotificationScheduleUpdateRequest(17, 0, true));

		mockMvc.perform(put(SCHEDULES_PATH)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content(SCHEDULE_UPDATE_BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_HOUR_NOT_ALLOWED"))
				.andDo(document("notification/schedule-update-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void enabled가_없으면_검증에_실패한다() throws Exception {
		mockMvc.perform(put(SCHEDULES_PATH)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"hour\": 17, \"minute\": 0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("enabled"));

		then(notificationRegister).shouldHaveNoInteractions();
	}

	@Test
	void 이벤트_알림_설정을_종류별로_응답한다() throws Exception {
		given(notificationEventSettingFinder.findEventSettings(42L)).willReturn(List.of(
				new NotificationEventSettingResult(NotificationEventType.AI_ANALYSIS, true),
				new NotificationEventSettingResult(NotificationEventType.COMMENT, false)
		));

		mockMvc.perform(get("/api/notifications/event-settings")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settings[0].type").value("AI_ANALYSIS"))
				.andExpect(jsonPath("$.settings[1].enabled").value(false))
				.andDo(document("notification/event-settings",
						responseFields(
								fieldWithPath("settings").description("이벤트 알림 종류별 설정"),
								fieldWithPath("settings[].type").description("알림 종류. AI_ANALYSIS·OPERATOR_MESSAGE·COMMENT·SUPPORT_REPLY"),
								fieldWithPath("settings[].enabled").description("해당 종류 수신 여부")
						)
				));
	}

	@Test
	void 이벤트_알림_설정을_바꾸면_204를_반환한다() throws Exception {
		mockMvc.perform(put("/api/notifications/event-settings")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\": \"COMMENT\", \"enabled\": false}"))
				.andExpect(status().isNoContent())
				.andDo(document("notification/event-setting-update",
						requestFields(fieldWithPath("type").description("알림 종류. AI_ANALYSIS·OPERATOR_MESSAGE·COMMENT·SUPPORT_REPLY"),
								fieldWithPath("enabled").description("해당 종류 수신 여부"))
				));

		then(notificationRegister).should().updateEventSetting(42L, new NotificationEventSettingUpdateRequest("COMMENT", false));
	}

	@Test
	void 없는_이벤트_종류면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new InvalidNotificationEventTypeException("MARKETING")).given(notificationRegister)
				.updateEventSetting(42L, new NotificationEventSettingUpdateRequest("MARKETING", true));

		mockMvc.perform(put("/api/notifications/event-settings")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-fid")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\": \"MARKETING\", \"enabled\": true}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_EVENT_TYPE_INVALID"))
				.andDo(document("notification/event-setting-update-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get(SCHEDULES_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(notificationScheduleFinder).shouldHaveNoInteractions();
	}
}
