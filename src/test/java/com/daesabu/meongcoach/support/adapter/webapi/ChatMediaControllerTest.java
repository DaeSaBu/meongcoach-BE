package com.daesabu.meongcoach.support.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.support.application.provided.ChatMediaUrlIssuer;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatMediaController.class)
@AutoConfigureRestDocs
class ChatMediaControllerTest {
	@Autowired MockMvc mvc;
	@MockitoBean ChatMediaUrlIssuer media;

	@Test
	void 로그인_회원의_상담_첨부_업로드_URL을_발급한다() throws Exception {
		given(media.upload(eq(42L), eq(17L), any())).willReturn(new MediaUploadUrlResult(
				"https://storage.googleapis.com/test/upload?signed=true", "https://storage.googleapis.com/test/file",
				"images/support-chat/42/photo.jpg", 600));
		mvc.perform(post("/api/support/conversations/{id}/attachments/upload-url", 17)
				.principal(() -> "42").header("Authorization", "Bearer access-token")
				.contentType("application/json").content("{\"mediaType\":\"IMAGE\",\"contentType\":\"image/jpeg\",\"fileSizeBytes\":1024}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.publicUrl").doesNotExist())
				.andDo(document("support/chat-media-upload",
						pathParameters(parameterWithName("id").description("대화 ID")),
						requestFields(fieldWithPath("mediaType").description("IMAGE 또는 VIDEO"),
								fieldWithPath("contentType").description("업로드할 파일 MIME 형식"),
								fieldWithPath("fileSizeBytes").description("파일 크기 바이트, 사진 20MiB·영상 512MiB 이하")),
						responseFields(fieldWithPath("uploadUrl").description("파일 PUT 업로드 URL"),
								fieldWithPath("objectKey").description("메시지 전송 시 첨부에 전달할 객체 키"),
								fieldWithPath("expiresInSeconds").description("업로드 URL 유효 시간 초"))));
	}

	@Test
	void 메시지_첨부의_다운로드_URL을_발급한다() throws Exception {
		given(media.download(42L, 17, 7)).willReturn("https://storage.googleapis.com/test/photo.jpg?signed=true");
		mvc.perform(get("/api/support/conversations/{id}/messages/{messageId}/attachment", 17, 7)
				.principal(() -> "42").header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andDo(document("support/chat-media-download",
						pathParameters(parameterWithName("id").description("대화 ID"), parameterWithName("messageId").description("메시지 ID")),
						responseFields(fieldWithPath("url").description("유효 시간이 있는 첨부 다운로드 URL"))));
		verify(media).download(42L, 17, 7);
	}

	@Test
	void 접근할_수_없는_첨부는_403이다() throws Exception {
		given(media.download(42L, 17, 7)).willThrow(new ChatAccessDeniedException());
		mvc.perform(get("/api/support/conversations/{id}/messages/{messageId}/attachment", 17, 7)
				.principal(() -> "42").header("Authorization", "Bearer access-token"))
				.andExpect(status().isForbidden())
				.andDo(document("support/chat-media-error", responseFields(
						fieldWithPath("title").description("HTTP 상태"), fieldWithPath("status").description("상태 코드"),
						fieldWithPath("detail").description("설명"), fieldWithPath("instance").description("요청 경로"),
						fieldWithPath("code").description("에러 코드"), fieldWithPath("timestamp").description("발생 시각"))));
	}
}
