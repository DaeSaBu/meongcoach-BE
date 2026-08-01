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
import com.daesabu.meongcoach.media.application.provided.VerifiedVideoResult;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.provided.VideoUploadVerifier;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.VideoAccessDeniedException;
import com.daesabu.meongcoach.media.domain.exception.VideoNotUploadedException;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
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

	private static final String VIDEO_OBJECT_KEY = "videos/ai-analysis/1/uuid.mp4";
	private static final String VIDEO_UPLOAD_URL =
			"https://test-account.r2.cloudflarestorage.com/test-bucket/" + VIDEO_OBJECT_KEY
					+ "?X-Amz-Expires=1800&X-Amz-Signature=example";
	private static final String VIDEO_PUBLIC_URL = "https://images.test.meongcoach.com/" + VIDEO_OBJECT_KEY;

	private static final String VIDEO_ISSUE_REQUEST = """
			{
				"target": "AI_ANALYSIS",
				"contentType": "video/mp4",
				"fileSizeBytes": 52428800
			}
			""";

	private static final String VIDEO_CONTENT_TYPE = "video/mp4";
	private static final long VIDEO_SIZE_BYTES = 52_428_800L;

	private static final String VIDEO_COMPLETION_REQUEST = """
			{
				"objectKey": "videos/ai-analysis/1/uuid.mp4"
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

	@Test
	@DisplayName("영상 업로드 URL을 발급한다")
	void issueVideoReturnsUploadUrl() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value(VIDEO_UPLOAD_URL))
				.andExpect(jsonPath("$.publicUrl").value(VIDEO_PUBLIC_URL))
				.andExpect(jsonPath("$.objectKey").value(VIDEO_OBJECT_KEY))
				.andExpect(jsonPath("$.expiresInSeconds").value(1800))
				.andDo(document("media/video-upload-url",
						requestFields(
								fieldWithPath("target").description("업로드 대상. 현재 `AI_ANALYSIS`(AI 영상 분석)만 지원"),
								fieldWithPath("contentType").description(
										"업로드할 영상의 Content-Type. `video/mp4`, `video/quicktime`만 지원"),
								fieldWithPath("fileSizeBytes").description(
										"업로드할 파일의 정확한 바이트 수. 1 이상 104857600(100MB) 이하")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. Content-Type과 Content-Length가 서명에 포함되므로 "
												+ "클라이언트는 PUT 시 두 헤더를 발급 요청에 보낸 contentType·fileSizeBytes와 "
												+ "정확히 일치시켜야 하며, 하나라도 다르면 R2가 403으로 거부한다"),
								fieldWithPath("publicUrl").description("업로드 완료 후 영상이 공개되는 URL"),
								fieldWithPath("objectKey").description(
										"스토리지 객체 키. 업로드를 마친 뒤 완료 확인 API에 이 값을 그대로 넘긴다"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	@DisplayName("지원하지 않는 영상 형식이면 400을 반환한다")
	void issueVideoFailsWhenContentTypeIsUnsupported() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("video/mp4", "video/avi")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_UNSUPPORTED_VIDEO_TYPE"))
				.andDo(document("media/video-upload-url-error",
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
	@DisplayName("파일 크기가 상한을 넘으면 400을 반환한다")
	void issueVideoFailsWhenFileSizeExceedsMax() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("52428800", String.valueOf(VideoFileSize.MAX_BYTES + 1))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_VIDEO_SIZE_EXCEEDED"));
	}

	@Test
	@DisplayName("지원하지 않는 영상 업로드 대상이면 400을 반환한다")
	void issueVideoFailsWhenTargetIsInvalid() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("AI_ANALYSIS", "BANNER")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_INVALID_UPLOAD_TARGET"));
	}

	@Test
	@DisplayName("파일 크기가 없으면 검증에 실패한다")
	void issueVideoFailsWhenFileSizeIsMissing() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"target\": \"AI_ANALYSIS\", \"contentType\": \"video/mp4\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));
	}

	@Test
	@DisplayName("영상 업로드 URL 발급 시 인증 정보가 없으면 401을 반환한다")
	void issueVideoFailsWithoutPrincipal() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("영상 업로드 완료를 확인한다")
	void verifyVideoUploadReturnsStoredVideo() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-completions")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_COMPLETION_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.objectKey").value(VIDEO_OBJECT_KEY))
				.andExpect(jsonPath("$.publicUrl").value(VIDEO_PUBLIC_URL))
				.andExpect(jsonPath("$.contentType").value(VIDEO_CONTENT_TYPE))
				.andExpect(jsonPath("$.sizeBytes").value(VIDEO_SIZE_BYTES))
				.andDo(document("media/video-upload-completion",
						requestFields(
								fieldWithPath("objectKey").description(
										"업로드 URL 발급 응답으로 받은 객체 키. 자기 소유의 키만 확인할 수 있다")
						),
						responseFields(
								fieldWithPath("objectKey").description("확인이 끝난 객체 키"),
								fieldWithPath("publicUrl").description(
										"영상이 공개되는 URL. AI 분석 요청 등 후속 API에는 이 값을 그대로 넘긴다"),
								fieldWithPath("contentType").description("R2가 보관 중인 객체의 실제 Content-Type"),
								fieldWithPath("sizeBytes").description(
										"객체의 실제 크기(바이트). 클라이언트가 발급 요청에 신고한 값이 아니라 R2가 보고한 값이다")
						)
				));
	}

	@Test
	@DisplayName("업로드되지 않은 영상 키면 404를 반환한다")
	void verifyVideoUploadFailsWhenNotUploaded() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-completions")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_COMPLETION_REQUEST.replace(VIDEO_OBJECT_KEY, "videos/ai-analysis/1/none.mp4")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEDIA_VIDEO_NOT_UPLOADED"))
				.andDo(document("media/video-upload-completion-error",
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
	@DisplayName("다른 사용자의 영상 키면 403을 반환한다")
	void verifyVideoUploadFailsWhenKeyBelongsToAnotherUser() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-completions")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_COMPLETION_REQUEST.replace(VIDEO_OBJECT_KEY, "videos/ai-analysis/2/uuid.mp4")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("MEDIA_VIDEO_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("형식이 깨진 영상 키면 400을 반환한다")
	void verifyVideoUploadFailsWhenObjectKeyIsMalformed() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-completions")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_COMPLETION_REQUEST.replace(VIDEO_OBJECT_KEY, "videos/1/uuid.mp4")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_INVALID_OBJECT_KEY"));
	}

	@Test
	@DisplayName("영상 업로드 완료 확인 시 인증 정보가 없으면 401을 반환한다")
	void verifyVideoUploadFailsWithoutPrincipal() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-completions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_COMPLETION_REQUEST))
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

		// 검증 순서는 VideoUploadUrlIssueService와 같게 둔다. 두 값이 동시에 틀린 요청의 에러 코드를 실제 구현과 맞추기 위해서다
		@Bean
		VideoUploadUrlIssuer videoUploadUrlIssuer() {
			return (userId, target, contentType, fileSizeBytes) -> {
				VideoUploadTarget.from(target);
				VideoType.fromContentType(contentType);
				VideoFileSize.of(fileSizeBytes);
				return new VideoUploadUrlResult(VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 1_800L);
			};
		}

		// 검증 순서 역시 VideoUploadVerifyService와 같게 둔다. 스토리지가 없으므로 VIDEO_OBJECT_KEY만 업로드된 것으로 취급한다
		@Bean
		VideoUploadVerifier videoUploadVerifier() {
			return (userId, objectKey) -> {
				VideoObjectKey key = VideoObjectKey.parse(objectKey);
				if (!key.belongsTo(userId)) {
					throw new VideoAccessDeniedException(key.value());
				}
				if (!VIDEO_OBJECT_KEY.equals(key.value())) {
					throw new VideoNotUploadedException(key.value());
				}
				return new VerifiedVideoResult(key.value(), VIDEO_PUBLIC_URL, VIDEO_CONTENT_TYPE, VIDEO_SIZE_BYTES);
			};
		}
	}
}
