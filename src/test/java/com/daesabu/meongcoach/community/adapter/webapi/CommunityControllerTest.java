package com.daesabu.meongcoach.community.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.PostFixture;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommunityController.class)
@AutoConfigureRestDocs
class CommunityControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostFinder postFinder;

	@Test
	void 게시글_목록을_다음_커서와_함께_반환한다() throws Exception {
		PostResult withMention = new PostResult(
				PostFixture.create(30L, 7L, "이 카드 따라해봤어요", LocalDateTime.of(2026, 10, 19, 10, 30, 0)),
				new UserProfileSummaryResult(7L, "멍멍이집사", "https://cdn.example.com/profile.png"),
				new MentionTargetResult(MentionTargetType.CARD, 101L, "앉아 준비"));
		PostResult withoutMention = new PostResult(
				PostFixture.create(29L, 8L, "오늘 산책 성공!", LocalDateTime.of(2026, 10, 19, 9, 0, 0)),
				new UserProfileSummaryResult(8L, "콩이엄마", "https://cdn.example.com/kong.png"),
				null);
		given(postFinder.findPosts(new PostListRequest(31L, 2)))
				.willReturn(new PostListResult(List.of(withMention, withoutMention), 29L));

		mockMvc.perform(get("/api/community/posts")
						.param("cursor", "31")
						.param("size", "2")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.posts[0].postId").value(30))
				.andExpect(jsonPath("$.posts[0].content").value("이 카드 따라해봤어요"))
				.andExpect(jsonPath("$.posts[0].author.userId").value(7))
				.andExpect(jsonPath("$.posts[0].author.nickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.posts[0].author.profileImageUrl").value("https://cdn.example.com/profile.png"))
				.andExpect(jsonPath("$.posts[0].mention.targetType").value("CARD"))
				.andExpect(jsonPath("$.posts[0].mention.targetId").value(101))
				.andExpect(jsonPath("$.posts[0].mention.title").value("앉아 준비"))
				.andExpect(jsonPath("$.posts[0].createdAt").value("2026-10-19T10:30:00"))
				.andExpect(jsonPath("$.posts[1].postId").value(29))
				.andExpect(jsonPath("$.posts[1].mention").value(nullValue()))
				.andExpect(jsonPath("$.nextCursor").value(29))
				.andDo(document("community/post-list",
						queryParameters(
								parameterWithName("cursor").optional()
										.description("이전 응답의 nextCursor. 첫 페이지는 생략하며, 이 ID보다 작은 게시글부터 조회한다"),
								parameterWithName("size").optional().description("한 번에 조회할 게시글 수. 1~50, 기본 20")
						),
						responseFields(
								fieldWithPath("posts[]").description("게시글 목록. 최신순(게시글 ID 내림차순)"),
								fieldWithPath("posts[].postId").description("게시글 ID"),
								fieldWithPath("posts[].content").description("게시글 본문"),
								fieldWithPath("posts[].author").optional()
										.description("작성자 프로필 요약. 작성자 프로필이 없으면 null"),
								fieldWithPath("posts[].author.userId").description("작성자 회원 ID"),
								fieldWithPath("posts[].author.nickname").description("작성자 닉네임"),
								fieldWithPath("posts[].author.profileImageUrl").description("작성자 프로필 이미지 URL"),
								fieldWithPath("posts[].mention").optional()
										.description("멘션 대상 요약. 멘션이 없거나 멘션 대상이 없으면 null"),
								fieldWithPath("posts[].mention.targetType").description("멘션 대상 종류. 현재 CARD(훈련 카드)만 있다"),
								fieldWithPath("posts[].mention.targetId").description("멘션 대상 ID"),
								fieldWithPath("posts[].mention.title").description("멘션 대상 제목"),
								fieldWithPath("posts[].createdAt").description("게시글 작성 시각(ISO-8601, 초 단위)"),
								fieldWithPath("nextCursor").optional()
										.description("다음 페이지 요청에 cursor로 보낼 값. 마지막 페이지면 null")
						)
				));
	}

	@Test
	void 커서와_size를_생략하면_첫_페이지를_기본_크기로_조회한다() throws Exception {
		given(postFinder.findPosts(any(PostListRequest.class))).willReturn(new PostListResult(List.of(), null));

		mockMvc.perform(get("/api/community/posts").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.posts").isEmpty())
				.andExpect(jsonPath("$.nextCursor").value(nullValue()));

		then(postFinder).should().findPosts(new PostListRequest(null, PostListRequest.DEFAULT_SIZE));
	}

	@Test
	void 작성자_프로필이나_멘션_대상이_없으면_null로_반환한다() throws Exception {
		PostResult orphan = new PostResult(
				PostFixture.create(1L, 7L, "본문", LocalDateTime.of(2026, 10, 19, 9, 0, 0)), null, null);
		given(postFinder.findPosts(any(PostListRequest.class))).willReturn(new PostListResult(List.of(orphan), null));

		mockMvc.perform(get("/api/community/posts").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.posts[0].postId").value(1))
				.andExpect(jsonPath("$.posts[0].author").value(nullValue()))
				.andExpect(jsonPath("$.posts[0].mention").value(nullValue()));
	}

	@Test
	void size가_허용_범위를_벗어나면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/community/posts")
						.param("size", "51")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("size"))
				.andDo(document("community/post-list-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)"),
								fieldWithPath("errors[].field").description("검증에 실패한 필드 이름"),
								fieldWithPath("errors[].message").description("검증 실패 사유")
						)
				));

		then(postFinder).shouldHaveNoInteractions();
	}

	@Test
	void cursor가_숫자가_아니면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/community/posts")
						.param("cursor", "abc")
						.principal(CURRENT_USER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("cursor"));

		then(postFinder).shouldHaveNoInteractions();
	}
}
