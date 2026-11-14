package com.daesabu.meongcoach.support.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.domain.ChatConversationFixture;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.ChatMessageFixture;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
@AutoConfigureRestDocs
class ChatControllerTest {
	@Autowired MockMvc mvc;
	@MockitoBean ChatRegister register;
	@MockitoBean ChatFinder finder;
	private final UUID clientId = UUID.fromString("cc23e1d4-c105-435e-9473-1f5e995967c1");

	private ChatMessage message() {
		return ChatMessageFixture.create(
				1, 17, 42, false, clientId, "문의합니다", Instant.parse("2026-11-01T00:00:00Z"));
	}

	private FieldDescriptor[] fields(String prefix) {
		return new FieldDescriptor[] {
			fieldWithPath(prefix + "id").description("메시지 커서"),
			fieldWithPath(prefix + "conversationId").description("대화 ID"),
			fieldWithPath(prefix + "senderId").description("발신 회원 ID"),
			fieldWithPath(prefix + "operator").description("상담원 답변 여부"),
			fieldWithPath(prefix + "clientMessageId").description("중복 방지 UUID"),
			fieldWithPath(prefix + "body").description("텍스트 본문"),
			fieldWithPath(prefix + "createdAt").description("생성 시각")
		};
	}

	@Test
	void 본인_대화를_연다() throws Exception {
		given(register.open(42L))
				.willReturn(ChatConversationFixture.create(17, 42, Instant.parse("2026-11-01T00:00:00Z")));
		mvc.perform(
						post("/api/support/conversations/me")
								.principal(() -> "42")
								.header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andDo(
						document(
								"support/chat-open",
								responseFields(
										fieldWithPath("id").description("대화 ID"),
										fieldWithPath("memberId").description("대화 소유 회원 ID"),
										fieldWithPath("updatedAt").description("최종 활동 시각"))));
	}

	@Test
	void 운영자_권한을_조회한다() throws Exception {
		given(finder.isOperator()).willReturn(true);
		mvc.perform(
						get("/api/support/conversations/capabilities")
								.principal(() -> "42")
								.header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andDo(
						document(
								"support/chat-capabilities",
								responseFields(
										fieldWithPath("operator").description("ADMIN·EXPERT 상담원 여부"))));
	}

	@Test
	void 상담원_대화목록을_조회한다() throws Exception {
		given(finder.conversations(0, null))
				.willReturn(
						List.of(ChatConversationFixture.create(17, 42, Instant.parse("2026-11-01T00:00:00Z"))));
		mvc.perform(
						get("/api/support/conversations")
								.principal(() -> "42")
								.header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andDo(
						document(
								"support/chat-list",
								queryParameters(
										parameterWithName("before").optional()
												.description(
														"이전 대화 ID, 최초 0; 최근 활동 시각·ID 내림차순 최대 50건"),
										parameterWithName("beforeUpdatedAt").optional().description("이전 응답 마지막 대화의 updatedAt 원문; before와 함께 전달")),
								responseFields(
										fieldWithPath("[].id").description("대화 ID"),
										fieldWithPath("[].memberId").description("대화 소유 회원 ID"),
										fieldWithPath("[].updatedAt").description("최종 활동 시각"))));
	}

	@Test
	void 메시지를_커서로_조회한다() throws Exception {
		given(finder.latestMessages(42L, 17)).willReturn(List.of(message()));
		mvc.perform(
						get("/api/support/conversations/{id}/messages", 17)
								.principal(() -> "42")
								.header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andDo(
						document(
								"support/chat-messages",
								pathParameters(parameterWithName("id").description("대화 ID")),
								responseFields(fields("[]."))));
	}

	@Test
	void 커서가_없으면_최신_메시지를_조회한다() throws Exception {
		given(finder.latestMessages(42L, 17)).willReturn(List.of(message()));
		mvc.perform(get("/api/support/conversations/17/messages").principal(() -> "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
		verify(finder).latestMessages(42L, 17);
	}

	@Test
	void after로_새_메시지를_조회한다() throws Exception {
		given(finder.newMessages(42L, 17, 0)).willReturn(List.of(message()));
		mvc.perform(get("/api/support/conversations/{id}/messages/new", 17)
				.param("after", "0").principal(() -> "42").header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andDo(document("support/chat-new-messages",
						pathParameters(parameterWithName("id").description("대화 ID")),
						queryParameters(parameterWithName("after").description("조회 기준 메시지 ID")),
						responseFields(fields("[]."))));
		verify(finder).newMessages(42L, 17, 0);
	}

	@Test
	void before로_이전_메시지를_조회한다() throws Exception {
		given(finder.previousMessages(42L, 17, 2)).willReturn(List.of(message()));
		mvc.perform(get("/api/support/conversations/{id}/messages/previous", 17)
				.param("before", "2").principal(() -> "42").header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andDo(document("support/chat-previous-messages",
						pathParameters(parameterWithName("id").description("대화 ID")),
						queryParameters(parameterWithName("before").description("조회 기준 메시지 ID")),
						responseFields(fields("[]."))));
		verify(finder).previousMessages(42L, 17, 2);
	}

	@Test
	void 메시지를_전송한다() throws Exception {
		given(register.send(eq(42L), eq(17L), any())).willReturn(message());
		mvc.perform(
						post("/api/support/conversations/{id}/messages", 17)
								.principal(() -> "42")
								.header("Authorization", "Bearer access-token")
								.contentType("application/json")
								.content(
										"{\"clientMessageId\":\""
												+ clientId
												+ "\",\"body\":\"문의합니다\"}"))
				.andExpect(status().isOk())
				.andDo(
						document(
								"support/chat-send",
								pathParameters(parameterWithName("id").description("대화 ID")),
								requestFields(
										fieldWithPath("clientMessageId")
												.description("재시도 시 유지할 UUID"),
										fieldWithPath("body").description("공백 제외 1~2000자 텍스트")),
								responseFields(fields(""))));
	}

	@Test
	void 존재하지_않는_대화는_403이다() throws Exception {
		given(finder.latestMessages(42L, 43)).willThrow(new ChatAccessDeniedException());
		mvc.perform(
						get("/api/support/conversations/{id}/messages", 43)
								.principal(() -> "42")
								.header("Authorization", "Bearer access-token"))
				.andExpect(status().isForbidden())
				.andDo(
						document(
								"support/chat-messages-error",
								responseFields(
										fieldWithPath("title").description("HTTP 상태"),
										fieldWithPath("status").description("상태 코드"),
										fieldWithPath("detail").description("설명"),
										fieldWithPath("instance").description("요청 경로"),
										fieldWithPath("code").description("에러 코드"),
										fieldWithPath("timestamp").description("발생 시각"))));
	}

	@Test
	void 빈_메시지는_400이다() throws Exception {
		mvc.perform(
						post("/api/support/conversations/17/messages")
								.principal(() -> "42")
								.contentType("application/json")
								.content(
										"{\"clientMessageId\":\""
												+ clientId
												+ "\",\"body\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 대화_커서의_마이크로초를_그대로_전달한다() throws Exception {
		Instant timestamp = Instant.parse("2026-11-01T00:00:00.123456Z");
		given(finder.conversations(42, timestamp)).willReturn(List.of());
		mvc.perform(get("/api/support/conversations")
				.param("before", "42").param("beforeUpdatedAt", timestamp.toString())
				.principal(() -> "42"))
				.andExpect(status().isOk());
		verify(finder).conversations(42, timestamp);
	}

	@Test
	void 신규와_이전_조회는_유효한_커서가_필요하다() throws Exception {
		mvc.perform(get("/api/support/conversations/17/messages/new").principal(() -> "42")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/support/conversations/17/messages/new").principal(() -> "42").param("after", "-1"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/support/conversations/17/messages/previous").principal(() -> "42")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/support/conversations/17/messages/previous").principal(() -> "42").param("before", "0"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(finder);
	}
}
