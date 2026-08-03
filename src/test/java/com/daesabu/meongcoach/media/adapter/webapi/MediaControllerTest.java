package com.daesabu.meongcoach.media.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
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

	private static final String VIDEO_OBJECT_KEY = "videos/training/1/uuid.mp4";
	private static final String VIDEO_UPLOAD_URL =
			"https://test-video-bucket.s3.ap-northeast-2.amazonaws.com/" + VIDEO_OBJECT_KEY
					+ "?X-Amz-Expires=900&X-Amz-Signature=example";
	private static final String VIDEO_PUBLIC_URL = "https://videos.test.meongcoach.com/" + VIDEO_OBJECT_KEY;

	private static final String ISSUE_REQUEST = """
			{
				"target": "USER_PROFILE",
				"contentType": "image/jpeg"
			}
			""";

	private static final String VIDEO_ISSUE_REQUEST = """
			{
				"target": "TRAINING_VIDEO",
				"contentType": "video/mp4",
				"fileSizeBytes": 10485760
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
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andDo(document("media/video-upload-url",
						requestFields(
								fieldWithPath("target").description("업로드 대상. 현재는 `TRAINING_VIDEO`(훈련 영상)만 지원"),
								fieldWithPath("contentType").description(
										"업로드할 영상의 Content-Type. `video/mp4`, `video/quicktime`만 지원"),
								fieldWithPath("fileSizeBytes").description(
										"업로드할 영상의 바이트 수. 1 이상 104857600(100MB) 이하여야 하며, "
												+ "이 값이 그대로 presigned URL의 Content-Length 서명에 들어간다")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. `Content-Type`은 요청한 값과, "
												+ "`Content-Length`는 요청한 `fileSizeBytes`와 정확히 같아야 한다"),
								fieldWithPath("publicUrl").description(
										"공개 도메인 기준의 영상 URL. 버킷을 비공개로 운영하면 직접 접근은 거부된다"),
								fieldWithPath("objectKey").description(
										"업로드된 객체의 키. 이후 API 요청에는 이 값을 담아 등록한다"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	@DisplayName("영상 파일 크기가 상한을 넘으면 400을 반환한다")
	void issueVideoFailsWhenFileSizeExceedsMax() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("10485760", "104857601")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_VIDEO_FILE_SIZE_EXCEEDED"))
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
	@DisplayName("영상 파일 크기가 0 이하면 400을 반환한다")
	void issueVideoFailsWhenFileSizeIsNotPositive() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("10485760", "0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_INVALID_VIDEO_FILE_SIZE"));
	}

	@Test
	@DisplayName("지원하지 않는 영상 형식이면 400을 반환한다")
	void issueVideoFailsWhenContentTypeIsUnsupported() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("video/mp4", "video/webm")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_UNSUPPORTED_VIDEO_TYPE"));
	}

	@Test
	@DisplayName("이미지 업로드 대상으로는 영상 URL을 발급하지 않는다")
	void issueVideoFailsWhenTargetIsInvalid() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("TRAINING_VIDEO", "USER_PROFILE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_INVALID_VIDEO_UPLOAD_TARGET"));
	}

	@Test
	@DisplayName("영상 파일 크기가 없으면 검증에 실패한다")
	void issueVideoFailsWhenFileSizeIsMissing() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"target\": \"TRAINING_VIDEO\", \"contentType\": \"video/mp4\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));
	}

	@Test
	@DisplayName("인증 정보가 없으면 영상 URL 발급도 401을 반환한다")
	void issueVideoFailsWithoutPrincipal() throws Exception {
		mockMvc.perform(post("/api/media/video-upload-urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
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

		@Bean
		VideoUploadUrlIssuer videoUploadUrlIssuer() {
			// 도메인 검증까지 컨트롤러 슬라이스에서 태워야 에러 코드 응답을 그대로 검증할 수 있다
			return (userId, target, contentType, fileSizeBytes) -> {
				VideoUploadTarget.from(target);
				VideoType.fromContentType(contentType);
				new VideoFileSize(fileSizeBytes);
				return new VideoUploadUrlResult(VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 900L);
			};
		}
	}
}
