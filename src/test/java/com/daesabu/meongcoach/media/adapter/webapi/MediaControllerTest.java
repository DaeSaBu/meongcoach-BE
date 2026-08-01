package com.daesabu.meongcoach.media.adapter.webapi;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MediaController.class)
@Import(MediaControllerTest.StubConfig.class)
@AutoConfigureRestDocs
@DisplayName("미디어 API")
class MediaControllerTest {

	private static final String UPLOAD_URL =
			"https://test-account.r2.cloudflarestorage.com/test-bucket/images/user-profile/1/uuid.jpg"
					+ "?X-Amz-Expires=600&X-Amz-Signature=example";
	private static final String PUBLIC_URL = "https://images.test.meongcoach.com/images/user-profile/1/uuid.jpg";

	private static final String ISSUE_REQUEST = """
			{
				"target": "USER_PROFILE",
				"contentType": "image/jpeg"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("이미지 업로드 URL을 발급한다")
	void issueReturnsUploadUrl() throws Exception {
		mockMvc.perform(post("/api/media/image-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ISSUE_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value(UPLOAD_URL))
				.andExpect(jsonPath("$.publicUrl").value(PUBLIC_URL))
				.andExpect(jsonPath("$.expiresInSeconds").value(600))
				.andDo(document("media/image-upload-url",
						requestFields(
								fieldWithPath("target").description(
										"업로드 대상. `USER_PROFILE`(사용자 프로필) 또는 `DOG_PROFILE`(강아지 프로필)"),
								fieldWithPath("contentType").description(
										"업로드할 이미지의 Content-Type. `image/jpeg`, `image/png`, `image/webp`만 지원")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"이미지를 PUT할 presigned URL. 요청한 Content-Type과 동일하게 업로드해야 한다"),
								fieldWithPath("publicUrl").description(
										"업로드 완료 후 이미지가 공개되는 URL. 온보딩 완료 요청 등에 이 값을 담아 등록한다"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	@DisplayName("지원하지 않는 이미지 형식이면 400을 반환한다")
	void issueFailsWhenContentTypeIsUnsupported() throws Exception {
		mockMvc.perform(post("/api/media/image-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ISSUE_REQUEST.replace("image/jpeg", "image/gif")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_UNSUPPORTED_IMAGE_TYPE"))
				.andDo(document("media/image-upload-url-error",
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
	@DisplayName("지원하지 않는 업로드 대상이면 400을 반환한다")
	void issueFailsWhenTargetIsInvalid() throws Exception {
		mockMvc.perform(post("/api/media/image-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ISSUE_REQUEST.replace("USER_PROFILE", "BANNER")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_INVALID_UPLOAD_TARGET"));
	}

	@Test
	@DisplayName("업로드 대상이 비어 있으면 검증에 실패한다")
	void issueFailsWhenTargetIsBlank() throws Exception {
		mockMvc.perform(post("/api/media/image-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"target\": \"\", \"contentType\": \"image/jpeg\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("target"));
	}

	@Test
	@DisplayName("인증 정보가 없으면 401을 반환한다")
	void issueFailsWithoutPrincipal() throws Exception {
		mockMvc.perform(post("/api/media/image-upload-urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ISSUE_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@TestConfiguration
	static class StubConfig {

		@Bean
		ImageUploadUrlIssuer imageUploadUrlIssuer() {
			return (userId, target, contentType) -> {
				ImageUploadTarget.from(target);
				ImageType.fromContentType(contentType);
				return new ImageUploadUrlResult(UPLOAD_URL, PUBLIC_URL, 600L);
			};
		}
	}
}
