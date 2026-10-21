package com.daesabu.meongcoach.community.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.community.application.provided.CommentRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.MentionRequest;
import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.domain.CommentFixture;
import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostFixture;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

	@MockitoBean
	private PostRegister postRegister;

	@MockitoBean
	private CommentRegister commentRegister;

	@Test
	void 게시글_목록을_다음_커서와_함께_반환한다() throws Exception {
		PostResult withMention = new PostResult(
				PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", LocalDateTime.of(2026, 10, 19, 10, 30, 0)),
				new UserProfileSummaryResult(7L, "멍멍이집사", "https://cdn.example.com/profile.png"),
				new MentionTargetResult(MentionTargetType.LESSON, 101L, "앉아 기초"));
		PostResult withoutMention = new PostResult(
				PostFixture.create(29L, 8L, "오늘 산책 성공!", LocalDateTime.of(2026, 10, 19, 9, 0, 0)),
				new UserProfileSummaryResult(8L, "콩이엄마", "https://cdn.example.com/kong.png"),
				null);
		given(postFinder.findPosts(new PostListRequest(31L, 2, 12L)))
				.willReturn(new PostListResult(List.of(withMention, withoutMention), 29L));

		mockMvc.perform(get("/api/community/posts")
						.param("cursor", "31")
						.param("size", "2")
						.param("lessonId", "12")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.posts[0].postId").value(30))
				.andExpect(jsonPath("$.posts[0].content").value("이 레슨 따라해봤어요"))
				.andExpect(jsonPath("$.posts[0].author.userId").value(7))
				.andExpect(jsonPath("$.posts[0].author.nickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.posts[0].author.profileImageUrl").value("https://cdn.example.com/profile.png"))
				.andExpect(jsonPath("$.posts[0].mention.targetType").value("LESSON"))
				.andExpect(jsonPath("$.posts[0].mention.targetId").value(101))
				.andExpect(jsonPath("$.posts[0].mention.title").value("앉아 기초"))
				.andExpect(jsonPath("$.posts[0].createdAt").value("2026-10-19T10:30:00"))
				.andExpect(jsonPath("$.posts[1].postId").value(29))
				.andExpect(jsonPath("$.posts[1].mention").value(nullValue()))
				.andExpect(jsonPath("$.nextCursor").value(29))
				.andDo(document("community/post-list",
						queryParameters(
								parameterWithName("cursor").optional()
										.description("이전 응답의 nextCursor. 첫 페이지는 생략하며, 이 ID보다 작은 게시글부터 조회한다"),
								parameterWithName("size").optional().description("한 번에 조회할 게시글 수. 1~50, 기본 20"),
								parameterWithName("lessonId").optional()
										.description("이 레슨을 멘션한 게시글만 조회한다. 생략하면 전체 게시글을 조회한다")
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
								fieldWithPath("posts[].mention.targetType").description("멘션 대상 종류. 현재 LESSON(레슨)만 있다"),
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

		then(postFinder).should().findPosts(new PostListRequest(null, PostListRequest.DEFAULT_SIZE, null));
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

	@Test
	void lessonId가_숫자가_아니면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/community/posts")
						.param("lessonId", "abc")
						.principal(CURRENT_USER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("lessonId"));

		then(postFinder).shouldHaveNoInteractions();
	}

	@Test
	void 게시글을_작성하면_201과_작성한_게시글을_반환한다() throws Exception {
		PostRegisterRequest request = new PostRegisterRequest("이 레슨 따라해봤어요", new MentionRequest("LESSON", 101L));
		given(postRegister.register(42L, request)).willReturn(PostFixture.create(30L, 42L, "이 레슨 따라해봤어요",
				new MentionCommand(MentionTargetType.LESSON, 101L), LocalDateTime.of(2026, 10, 20, 10, 30, 0)));

		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "이 레슨 따라해봤어요", "mention": {"targetType": "LESSON", "targetId": 101}}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId").value(30))
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.content").value("이 레슨 따라해봤어요"))
				.andExpect(jsonPath("$.mention.targetType").value("LESSON"))
				.andExpect(jsonPath("$.mention.targetId").value(101))
				.andExpect(jsonPath("$.createdAt").value("2026-10-20T10:30:00"))
				.andDo(document("community/post-register",
						requestFields(
								fieldWithPath("content").description("필수 입력. 게시글 본문. 공백만으로 채울 수 없고 500자 이하"),
								fieldWithPath("mention").optional().description("멘션 대상. 멘션 없이 작성하면 생략한다"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류. 현재 LESSON(레슨)만 받는다"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID(양의 정수)")
						),
						responseFields(
								fieldWithPath("postId").description("작성한 게시글 ID"),
								fieldWithPath("userId").description("작성자 회원 ID"),
								fieldWithPath("content").description("게시글 본문"),
								fieldWithPath("mention").optional().description("멘션 대상. 멘션 없이 작성했으면 null"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID"),
								fieldWithPath("createdAt").description("게시글 작성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void 멘션_없이_작성하면_mention을_null로_반환한다() throws Exception {
		given(postRegister.register(42L, new PostRegisterRequest("오늘 산책 성공!", null)))
				.willReturn(PostFixture.create(31L, 42L, "오늘 산책 성공!", LocalDateTime.of(2026, 10, 20, 11, 0, 0)));

		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "오늘 산책 성공!"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId").value(31))
				.andExpect(jsonPath("$.mention").value(nullValue()));
	}

	@Test
	void 멘션한_레슨이_없으면_404를_반환한다() throws Exception {
		PostRegisterRequest request = new PostRegisterRequest("이 레슨 따라해봤어요", new MentionRequest("LESSON", 999L));
		given(postRegister.register(42L, request)).willThrow(new MentionTargetNotFoundException(999L));

		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "이 레슨 따라해봤어요", "mention": {"targetType": "LESSON", "targetId": 999}}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_MENTION_TARGET_NOT_FOUND"))
				.andDo(document("community/post-register-error",
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
	void 멘션_대상_종류가_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "본문", "mention": {"targetType": " ", "targetId": 101}}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("mention.targetType"));

		then(postRegister).shouldHaveNoInteractions();
	}

	@Test
	void 댓글을_작성하면_201과_작성한_댓글을_반환한다() throws Exception {
		Post post = PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", LocalDateTime.of(2026, 10, 20, 10, 30, 0));
		given(commentRegister.register(42L, 30L, new CommentRegisterRequest("저희 강아지도 성공했어요!", 100L)))
				.willReturn(CommentFixture.create(101L, post, 42L, "저희 강아지도 성공했어요!",
						LocalDateTime.of(2026, 10, 21, 9, 0, 0)));

		mockMvc.perform(post("/api/community/posts/{postId}/comments", 30L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "저희 강아지도 성공했어요!", "parentId": 100}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.commentId").value(101))
				.andExpect(jsonPath("$.postId").value(30))
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.content").value("저희 강아지도 성공했어요!"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-21T09:00:00"))
				.andDo(document("community/comment-register",
						pathParameters(parameterWithName("postId").description("댓글을 작성할 게시글 ID")),
						requestFields(
								fieldWithPath("content").description("필수 입력. 댓글 본문. 공백만으로 채울 수 없고 500자 이하"),
								fieldWithPath("parentId").optional()
										.description("답글을 달 댓글 ID(양의 정수). 답글은 그 댓글의 루트 댓글에 대한 답글로 작성된다. 댓글이면 생략한다")
						),
						responseFields(
								fieldWithPath("commentId").description("작성한 댓글 ID"),
								fieldWithPath("postId").description("게시글 ID"),
								fieldWithPath("userId").description("작성자 회원 ID"),
								fieldWithPath("content").description("댓글 본문"),
								fieldWithPath("createdAt").description("댓글 작성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void parentId_없이_작성하면_루트_댓글로_작성을_요청한다() throws Exception {
		Post post = PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", LocalDateTime.of(2026, 10, 20, 10, 30, 0));
		given(commentRegister.register(42L, 30L, new CommentRegisterRequest("축하해요!", null)))
				.willReturn(CommentFixture.create(100L, post, 42L, "축하해요!", LocalDateTime.of(2026, 10, 21, 9, 0, 0)));

		mockMvc.perform(post("/api/community/posts/{postId}/comments", 30L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "축하해요!"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.commentId").value(100));

		then(commentRegister).should().register(42L, 30L, new CommentRegisterRequest("축하해요!", null));
	}

	@Test
	void 댓글을_작성할_게시글이_없으면_404를_반환한다() throws Exception {
		given(commentRegister.register(42L, 999L, new CommentRegisterRequest("축하해요!", null)))
				.willThrow(new PostNotFoundException(999L));

		mockMvc.perform(post("/api/community/posts/{postId}/comments", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "축하해요!"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_POST_NOT_FOUND"))
				.andDo(document("community/comment-register-error",
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
	void parentId가_양수가_아니면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/community/posts/{postId}/comments", 30L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "축하해요!", "parentId": 0}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("parentId"));

		then(commentRegister).shouldHaveNoInteractions();
	}
}
