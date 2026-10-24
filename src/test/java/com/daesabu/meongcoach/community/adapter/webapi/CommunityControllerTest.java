package com.daesabu.meongcoach.community.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.CommentRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.PostMediaUploadUrlIssuer;
import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentUpdateRequest;
import com.daesabu.meongcoach.community.application.provided.dto.MentionRequest;
import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostUpdateRequest;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyResult;
import com.daesabu.meongcoach.community.domain.CommentFixture;
import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostFixture;
import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import com.daesabu.meongcoach.community.domain.PostMediaType;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
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

	@MockitoBean
	private CommentFinder commentFinder;

	@MockitoBean
	private PostMediaUploadUrlIssuer postMediaUploadUrlIssuer;

	@Test
	void 게시글_목록을_다음_커서와_함께_반환한다() throws Exception {
		List<PostMediaCommand> media = List.of(
				new PostMediaCommand(PostMediaType.IMAGE, "https://media.meongcoach.com/images/community-post/7/photo.jpg"),
				new PostMediaCommand(PostMediaType.VIDEO, "https://media.meongcoach.com/videos/community-post/7/clip.mp4"));
		PostResult withMention = new PostResult(
				PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", null, media, LocalDateTime.of(2026, 10, 19, 10, 30, 0)),
				new AuthorResult(7L, "초코", "https://media.meongcoach.com/images/dog-profile/7/choco.jpg"),
				new MentionTargetResult(MentionTargetType.LESSON, 101L, "앉아 기초"));
		PostResult withoutMention = new PostResult(
				PostFixture.create(29L, 8L, "오늘 산책 성공!", LocalDateTime.of(2026, 10, 19, 9, 0, 0)),
				new AuthorResult(8L, "콩이", "https://media.meongcoach.com/images/dog-profile/8/kong.jpg"),
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
				.andExpect(jsonPath("$.posts[0].author.nickname").value("초코 보호자"))
				.andExpect(jsonPath("$.posts[0].author.profileImageUrl").value("https://media.meongcoach.com/images/dog-profile/7/choco.jpg"))
				.andExpect(jsonPath("$.posts[0].mention.targetType").value("LESSON"))
				.andExpect(jsonPath("$.posts[0].mention.targetId").value(101))
				.andExpect(jsonPath("$.posts[0].mention.title").value("앉아 기초"))
				.andExpect(jsonPath("$.posts[0].media[0].type").value("IMAGE"))
				.andExpect(jsonPath("$.posts[0].media[0].url")
						.value("https://media.meongcoach.com/images/community-post/7/photo.jpg"))
				.andExpect(jsonPath("$.posts[0].media[1].type").value("VIDEO"))
				.andExpect(jsonPath("$.posts[0].media[1].url")
						.value("https://media.meongcoach.com/videos/community-post/7/clip.mp4"))
				.andExpect(jsonPath("$.posts[0].createdAt").value("2026-10-19T10:30:00"))
				.andExpect(jsonPath("$.posts[1].postId").value(29))
				.andExpect(jsonPath("$.posts[1].mention").value(nullValue()))
				.andExpect(jsonPath("$.posts[1].media").isEmpty())
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
										.description("작성자 요약. 작성자가 탈퇴했거나 등록한 강아지가 없으면 null"),
								fieldWithPath("posts[].author.userId").description("작성자 회원 ID"),
								fieldWithPath("posts[].author.nickname").description("작성자 표시 이름. 작성자가 첫 번째로 등록한 강아지 이름 뒤에 ' 보호자'를 붙인다"),
								fieldWithPath("posts[].author.profileImageUrl").description("작성자가 첫 번째로 등록한 강아지의 프로필 이미지 URL. 이미지를 등록하지 않았으면 빈 문자열"),
								fieldWithPath("posts[].mention").optional()
										.description("멘션 대상 요약. 멘션이 없거나 멘션 대상이 없으면 null"),
								fieldWithPath("posts[].mention.targetType").description("멘션 대상 종류. 현재 LESSON(레슨)만 있다"),
								fieldWithPath("posts[].mention.targetId").description("멘션 대상 ID"),
								fieldWithPath("posts[].mention.title").description("멘션 대상 제목"),
								fieldWithPath("posts[].media[]").description("게시글 사진·영상 목록. 배열 순서가 표시 순서이고, 없으면 빈 배열"),
								fieldWithPath("posts[].media[].type").description("미디어 종류. IMAGE(사진), VIDEO(영상)"),
								fieldWithPath("posts[].media[].url").description("미디어 공개 URL"),
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
	void 게시글_하나를_반환한다() throws Exception {
		List<PostMediaCommand> media = List.of(
				new PostMediaCommand(PostMediaType.IMAGE, "https://media.meongcoach.com/images/community-post/7/photo.jpg"));
		PostResult result = new PostResult(
				PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", null, media, LocalDateTime.of(2026, 10, 19, 10, 30, 0)),
				new AuthorResult(7L, "초코", "https://media.meongcoach.com/images/dog-profile/7/choco.jpg"),
				new MentionTargetResult(MentionTargetType.LESSON, 101L, "앉아 기초"));
		given(postFinder.findPostDetail(30L)).willReturn(result);

		mockMvc.perform(get("/api/community/posts/{postId}", 30L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(30))
				.andExpect(jsonPath("$.content").value("이 레슨 따라해봤어요"))
				.andExpect(jsonPath("$.author.userId").value(7))
				.andExpect(jsonPath("$.author.nickname").value("초코 보호자"))
				.andExpect(jsonPath("$.mention.targetType").value("LESSON"))
				.andExpect(jsonPath("$.mention.title").value("앉아 기초"))
				.andExpect(jsonPath("$.media[0].type").value("IMAGE"))
				.andExpect(jsonPath("$.media[0].url")
						.value("https://media.meongcoach.com/images/community-post/7/photo.jpg"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-19T10:30:00"))
				.andDo(document("community/post-detail",
						pathParameters(parameterWithName("postId").description("조회할 게시글 ID")),
						responseFields(
								fieldWithPath("postId").description("게시글 ID"),
								fieldWithPath("content").description("게시글 본문"),
								fieldWithPath("author").optional()
										.description("작성자 요약. 작성자가 탈퇴했거나 등록한 강아지가 없으면 null"),
								fieldWithPath("author.userId").description("작성자 회원 ID"),
								fieldWithPath("author.nickname").description("작성자 표시 이름. 작성자가 첫 번째로 등록한 강아지 이름 뒤에 ' 보호자'를 붙인다"),
								fieldWithPath("author.profileImageUrl").description("작성자가 첫 번째로 등록한 강아지의 프로필 이미지 URL. 이미지를 등록하지 않았으면 빈 문자열"),
								fieldWithPath("mention").optional()
										.description("멘션 대상 요약. 멘션이 없거나 멘션 대상이 없으면 null"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류. 현재 LESSON(레슨)만 있다"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID"),
								fieldWithPath("mention.title").description("멘션 대상 제목"),
								fieldWithPath("media[]").description("게시글 사진·영상 목록. 배열 순서가 표시 순서이고, 없으면 빈 배열"),
								fieldWithPath("media[].type").description("미디어 종류. IMAGE(사진), VIDEO(영상)"),
								fieldWithPath("media[].url").description("미디어 공개 URL"),
								fieldWithPath("createdAt").description("게시글 작성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void 게시글이_없으면_404를_반환한다() throws Exception {
		given(postFinder.findPostDetail(999L)).willThrow(new PostNotFoundException(999L));

		mockMvc.perform(get("/api/community/posts/{postId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_POST_NOT_FOUND"))
				.andDo(document("community/post-detail-error",
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
	void 게시글을_작성하면_201과_작성한_게시글을_반환한다() throws Exception {
		String imageUrl = "https://media.meongcoach.com/images/community-post/42/photo.jpg";
		String videoUrl = "https://media.meongcoach.com/videos/community-post/42/clip.mp4";
		PostRegisterRequest request = new PostRegisterRequest("이 레슨 따라해봤어요", new MentionRequest("LESSON", 101L),
				List.of(new PostMediaRequest("IMAGE", imageUrl), new PostMediaRequest("VIDEO", videoUrl)));
		List<PostMediaCommand> media = List.of(
				new PostMediaCommand(PostMediaType.IMAGE, imageUrl),
				new PostMediaCommand(PostMediaType.VIDEO, videoUrl));
		given(postRegister.register(42L, request)).willReturn(PostFixture.create(30L, 42L, "이 레슨 따라해봤어요",
				new MentionCommand(MentionTargetType.LESSON, 101L), media, LocalDateTime.of(2026, 10, 20, 10, 30, 0)));

		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "이 레슨 따라해봤어요",
								  "mention": {"targetType": "LESSON", "targetId": 101},
								  "media": [
								    {"type": "IMAGE", "url": "https://media.meongcoach.com/images/community-post/42/photo.jpg"},
								    {"type": "VIDEO", "url": "https://media.meongcoach.com/videos/community-post/42/clip.mp4"}
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId").value(30))
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.content").value("이 레슨 따라해봤어요"))
				.andExpect(jsonPath("$.mention.targetType").value("LESSON"))
				.andExpect(jsonPath("$.mention.targetId").value(101))
				.andExpect(jsonPath("$.media[0].type").value("IMAGE"))
				.andExpect(jsonPath("$.media[0].url").value(imageUrl))
				.andExpect(jsonPath("$.media[1].type").value("VIDEO"))
				.andExpect(jsonPath("$.media[1].url").value(videoUrl))
				.andExpect(jsonPath("$.createdAt").value("2026-10-20T10:30:00"))
				.andDo(document("community/post-register",
						requestFields(
								fieldWithPath("content").description("필수 입력. 게시글 본문. 공백만으로 채울 수 없고 500자 이하"),
								fieldWithPath("mention").optional().description("멘션 대상. 멘션 없이 작성하면 생략한다"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류. 현재 LESSON(레슨)만 받는다"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID(양의 정수)"),
								fieldWithPath("media").optional()
										.description("첨부할 사진·영상. 배열 순서가 표시 순서이고, 사진 3개·영상 2개까지. 첨부 없이 작성하면 생략한다"),
								fieldWithPath("media[].type").description("미디어 종류. IMAGE(사진), VIDEO(영상)"),
								fieldWithPath("media[].url")
										.description("게시글 미디어 업로드 URL 발급 응답의 공개 URL. 본인이 게시글 용도로 올린 파일만 받는다")
						),
						responseFields(
								fieldWithPath("postId").description("작성한 게시글 ID"),
								fieldWithPath("userId").description("작성자 회원 ID"),
								fieldWithPath("content").description("게시글 본문"),
								fieldWithPath("mention").optional().description("멘션 대상. 멘션 없이 작성했으면 null"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID"),
								fieldWithPath("media[]").description("첨부한 사진·영상 목록. 요청 순서대로이고, 첨부 없이 작성했으면 빈 배열"),
								fieldWithPath("media[].type").description("미디어 종류. IMAGE(사진), VIDEO(영상)"),
								fieldWithPath("media[].url").description("미디어 공개 URL"),
								fieldWithPath("createdAt").description("게시글 작성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void 멘션_없이_작성하면_mention을_null로_반환한다() throws Exception {
		given(postRegister.register(42L, new PostRegisterRequest("오늘 산책 성공!", null, null)))
				.willReturn(PostFixture.create(31L, 42L, "오늘 산책 성공!", LocalDateTime.of(2026, 10, 20, 11, 0, 0)));

		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "오늘 산책 성공!"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId").value(31))
				.andExpect(jsonPath("$.mention").value(nullValue()))
				.andExpect(jsonPath("$.media").isEmpty());
	}

	@Test
	void 멘션한_레슨이_없으면_404를_반환한다() throws Exception {
		PostRegisterRequest request = new PostRegisterRequest("이 레슨 따라해봤어요", new MentionRequest("LESSON", 999L), null);
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
	void 미디어에_url이_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "본문", "media": [{"type": "IMAGE", "url": " "}]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("media[0].url"));

		then(postRegister).shouldHaveNoInteractions();
	}

	@Test
	void 첨부한_사진이_3개를_넘으면_400을_반환한다() throws Exception {
		given(postRegister.register(eq(42L), any(PostRegisterRequest.class)))
				.willThrow(new PostMediaCountExceededException(PostMediaType.IMAGE));

		mockMvc.perform(post("/api/community/posts")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "본문", "media": [
								  {"type": "IMAGE", "url": "https://media.meongcoach.com/images/community-post/42/1.jpg"},
								  {"type": "IMAGE", "url": "https://media.meongcoach.com/images/community-post/42/2.jpg"},
								  {"type": "IMAGE", "url": "https://media.meongcoach.com/images/community-post/42/3.jpg"},
								  {"type": "IMAGE", "url": "https://media.meongcoach.com/images/community-post/42/4.jpg"}
								]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMUNITY_POST_MEDIA_COUNT_EXCEEDED"));
	}

	@Test
	void 게시글을_수정하면_200과_수정한_게시글을_반환한다() throws Exception {
		String imageUrl = "https://media.meongcoach.com/images/community-post/42/photo.jpg";
		String videoUrl = "https://media.meongcoach.com/videos/community-post/42/clip.mp4";
		PostUpdateRequest request = new PostUpdateRequest("레슨 다시 따라해봤어요", new MentionRequest("LESSON", 102L),
				List.of(new PostMediaRequest("VIDEO", videoUrl), new PostMediaRequest("IMAGE", imageUrl)));
		List<PostMediaCommand> media = List.of(
				new PostMediaCommand(PostMediaType.VIDEO, videoUrl),
				new PostMediaCommand(PostMediaType.IMAGE, imageUrl));
		given(postRegister.update(42L, 30L, request)).willReturn(PostFixture.create(30L, 42L, "레슨 다시 따라해봤어요",
				new MentionCommand(MentionTargetType.LESSON, 102L), media, LocalDateTime.of(2026, 10, 20, 10, 30, 0)));

		mockMvc.perform(put("/api/community/posts/{postId}", 30L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "레슨 다시 따라해봤어요",
								  "mention": {"targetType": "LESSON", "targetId": 102},
								  "media": [
								    {"type": "VIDEO", "url": "https://media.meongcoach.com/videos/community-post/42/clip.mp4"},
								    {"type": "IMAGE", "url": "https://media.meongcoach.com/images/community-post/42/photo.jpg"}
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(30))
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.content").value("레슨 다시 따라해봤어요"))
				.andExpect(jsonPath("$.mention.targetType").value("LESSON"))
				.andExpect(jsonPath("$.mention.targetId").value(102))
				.andExpect(jsonPath("$.media[0].type").value("VIDEO"))
				.andExpect(jsonPath("$.media[0].url").value(videoUrl))
				.andExpect(jsonPath("$.media[1].type").value("IMAGE"))
				.andExpect(jsonPath("$.media[1].url").value(imageUrl))
				.andExpect(jsonPath("$.createdAt").value("2026-10-20T10:30:00"))
				.andDo(document("community/post-update",
						pathParameters(parameterWithName("postId").description("수정할 게시글 ID. 본인이 작성한 게시글만 수정할 수 있다")),
						requestFields(
								fieldWithPath("content").description("필수 입력. 게시글 본문. 공백만으로 채울 수 없고 500자 이하"),
								fieldWithPath("mention").optional().description("멘션 대상. 생략하면 기존 멘션을 제거한다"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류. 현재 LESSON(레슨)만 받는다"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID(양의 정수)"),
								fieldWithPath("media").optional()
										.description("수정 후 남길 사진·영상 전체. 배열 순서가 표시 순서이고, 사진 3개·영상 2개까지. 생략하면 기존 미디어를 모두 제거한다"),
								fieldWithPath("media[].type").description("미디어 종류. IMAGE(사진), VIDEO(영상)"),
								fieldWithPath("media[].url")
										.description("기존 미디어 URL 또는 게시글 미디어 업로드 URL 발급 응답의 공개 URL. 본인이 게시글 용도로 올린 파일만 받는다")
						),
						responseFields(
								fieldWithPath("postId").description("수정한 게시글 ID"),
								fieldWithPath("userId").description("작성자 회원 ID"),
								fieldWithPath("content").description("게시글 본문"),
								fieldWithPath("mention").optional().description("멘션 대상. 멘션 없이 수정했으면 null"),
								fieldWithPath("mention.targetType").description("멘션 대상 종류"),
								fieldWithPath("mention.targetId").description("멘션 대상 ID"),
								fieldWithPath("media[]").description("사진·영상 목록. 요청 순서대로이고, 미디어 없이 수정했으면 빈 배열"),
								fieldWithPath("media[].type").description("미디어 종류. IMAGE(사진), VIDEO(영상)"),
								fieldWithPath("media[].url").description("미디어 공개 URL"),
								fieldWithPath("createdAt").description("게시글 작성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void 멘션과_미디어_없이_수정하면_mention을_null로_반환한다() throws Exception {
		given(postRegister.update(42L, 31L, new PostUpdateRequest("오늘 산책 성공!", null, null)))
				.willReturn(PostFixture.create(31L, 42L, "오늘 산책 성공!", LocalDateTime.of(2026, 10, 20, 11, 0, 0)));

		mockMvc.perform(put("/api/community/posts/{postId}", 31L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "오늘 산책 성공!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(31))
				.andExpect(jsonPath("$.mention").value(nullValue()))
				.andExpect(jsonPath("$.media").isEmpty());
	}

	@Test
	void 수정할_게시글이_없거나_본인_게시글이_아니면_404를_반환한다() throws Exception {
		given(postRegister.update(42L, 999L, new PostUpdateRequest("본문", null, null)))
				.willThrow(new PostNotFoundException(999L));

		mockMvc.perform(put("/api/community/posts/{postId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "본문"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_POST_NOT_FOUND"))
				.andDo(document("community/post-update-error",
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
	void 수정_요청의_미디어에_url이_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(put("/api/community/posts/{postId}", 30L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "본문", "media": [{"type": "IMAGE", "url": " "}]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("media[0].url"));

		then(postRegister).shouldHaveNoInteractions();
	}

	@Test
	void 게시글을_삭제하면_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/community/posts/{postId}", 30L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNoContent())
				.andDo(document("community/post-delete",
						pathParameters(parameterWithName("postId").description("삭제할 게시글 ID. 본인이 작성한 게시글만 삭제할 수 있다"))
				));
	}

	@Test
	void 인증_주체와_경로의_게시글_ID로_삭제를_위임한다() throws Exception {
		mockMvc.perform(delete("/api/community/posts/{postId}", 30L).principal(CURRENT_USER))
				.andExpect(status().isNoContent());

		then(postRegister).should().delete(42L, 30L);
	}

	@Test
	void 삭제할_게시글이_없거나_본인_게시글이_아니면_404를_반환한다() throws Exception {
		willThrow(new PostNotFoundException(999L)).given(postRegister).delete(42L, 999L);

		mockMvc.perform(delete("/api/community/posts/{postId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_POST_NOT_FOUND"))
				.andDo(document("community/post-delete-error",
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

	@Test
	void 댓글을_수정하면_200과_수정한_댓글을_반환한다() throws Exception {
		Post post = PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", LocalDateTime.of(2026, 10, 20, 10, 30, 0));
		given(commentRegister.update(42L, 101L, new CommentUpdateRequest("저희 강아지도 두 번째에 성공했어요!")))
				.willReturn(CommentFixture.create(101L, post, 42L, "저희 강아지도 두 번째에 성공했어요!",
						LocalDateTime.of(2026, 10, 21, 9, 0, 0)));

		mockMvc.perform(put("/api/community/comments/{commentId}", 101L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "저희 강아지도 두 번째에 성공했어요!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.commentId").value(101))
				.andExpect(jsonPath("$.postId").value(30))
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.content").value("저희 강아지도 두 번째에 성공했어요!"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-21T09:00:00"))
				.andDo(document("community/comment-update",
						pathParameters(parameterWithName("commentId").description("수정할 댓글 ID. 본인이 작성한 댓글·답글만 수정할 수 있다")),
						requestFields(
								fieldWithPath("content").description("필수 입력. 댓글 본문. 공백만으로 채울 수 없고 500자 이하")
						),
						responseFields(
								fieldWithPath("commentId").description("수정한 댓글 ID"),
								fieldWithPath("postId").description("게시글 ID"),
								fieldWithPath("userId").description("작성자 회원 ID"),
								fieldWithPath("content").description("댓글 본문"),
								fieldWithPath("createdAt").description("댓글 작성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void 수정할_댓글이_없거나_본인_댓글이_아니면_404를_반환한다() throws Exception {
		given(commentRegister.update(42L, 999L, new CommentUpdateRequest("축하해요!")))
				.willThrow(new CommentNotFoundException(999L));

		mockMvc.perform(put("/api/community/comments/{commentId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": "축하해요!"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_NOT_FOUND"))
				.andDo(document("community/comment-update-error",
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
	void 수정할_댓글_본문이_올바르지_않으면_400을_반환한다() throws Exception {
		given(commentRegister.update(42L, 101L, new CommentUpdateRequest(" ")))
				.willThrow(new InvalidCommentContentException("본문이 비어 있습니다."));

		mockMvc.perform(put("/api/community/comments/{commentId}", 101L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content": " "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_CONTENT_INVALID"));
	}

	@Test
	void 댓글을_삭제하면_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/community/comments/{commentId}", 101L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNoContent())
				.andDo(document("community/comment-delete",
						pathParameters(parameterWithName("commentId").description("삭제할 댓글 ID. 본인이 작성한 댓글·답글만 삭제할 수 있다"))
				));
	}

	@Test
	void 인증_주체와_경로의_댓글_ID로_삭제를_위임한다() throws Exception {
		mockMvc.perform(delete("/api/community/comments/{commentId}", 101L).principal(CURRENT_USER))
				.andExpect(status().isNoContent());

		then(commentRegister).should().delete(42L, 101L);
	}

	@Test
	void 삭제할_댓글이_없거나_본인_댓글이_아니면_404를_반환한다() throws Exception {
		willThrow(new CommentNotFoundException(999L)).given(commentRegister).delete(42L, 999L);

		mockMvc.perform(delete("/api/community/comments/{commentId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_NOT_FOUND"))
				.andDo(document("community/comment-delete-error",
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
	void 댓글_목록을_다음_커서와_함께_반환한다() throws Exception {
		Post post = PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", LocalDateTime.of(2026, 10, 19, 10, 30, 0));
		CommentResult withProfile = new CommentResult(
				CommentFixture.create(100L, post, 7L, "저희 강아지도 성공했어요!", LocalDateTime.of(2026, 10, 19, 11, 0, 0)),
				new AuthorResult(7L, "초코", "https://media.meongcoach.com/images/dog-profile/7/choco.jpg"), 3L);
		CommentResult withoutProfile = new CommentResult(
				CommentFixture.create(101L, post, 8L, "축하해요!", LocalDateTime.of(2026, 10, 19, 12, 0, 0)), null, 0L);
		given(commentFinder.findComments(30L, new CommentListRequest(99L, 2)))
				.willReturn(new CommentListResult(List.of(withProfile, withoutProfile), 101L));

		mockMvc.perform(get("/api/community/posts/{postId}/comments", 30L)
						.param("cursor", "99")
						.param("size", "2")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comments[0].commentId").value(100))
				.andExpect(jsonPath("$.comments[0].content").value("저희 강아지도 성공했어요!"))
				.andExpect(jsonPath("$.comments[0].author.userId").value(7))
				.andExpect(jsonPath("$.comments[0].author.nickname").value("초코 보호자"))
				.andExpect(jsonPath("$.comments[0].replyCount").value(3))
				.andExpect(jsonPath("$.comments[0].createdAt").value("2026-10-19T11:00:00"))
				.andExpect(jsonPath("$.comments[1].commentId").value(101))
				.andExpect(jsonPath("$.comments[1].author").value(nullValue()))
				.andExpect(jsonPath("$.comments[1].replyCount").value(0))
				.andExpect(jsonPath("$.nextCursor").value(101))
				.andDo(document("community/comment-list",
						pathParameters(parameterWithName("postId").description("댓글을 조회할 게시글 ID")),
						queryParameters(
								parameterWithName("cursor").optional()
										.description("이전 응답의 nextCursor. 첫 페이지는 생략하며, 이 ID보다 큰 댓글부터 조회한다"),
								parameterWithName("size").optional().description("한 번에 조회할 댓글 수. 1~50, 기본 20")
						),
						responseFields(
								fieldWithPath("comments[]")
										.description("루트 댓글 목록. 오래된 순(댓글 ID 오름차순). 답글과 삭제된 댓글은 담기지 않는다"),
								fieldWithPath("comments[].commentId").description("댓글 ID"),
								fieldWithPath("comments[].content").description("댓글 본문"),
								fieldWithPath("comments[].author").optional()
										.description("작성자 요약. 작성자가 탈퇴했거나 등록한 강아지가 없으면 null"),
								fieldWithPath("comments[].author.userId").description("작성자 회원 ID"),
								fieldWithPath("comments[].author.nickname").description("작성자 표시 이름. 작성자가 첫 번째로 등록한 강아지 이름 뒤에 ' 보호자'를 붙인다"),
								fieldWithPath("comments[].author.profileImageUrl").description("작성자가 첫 번째로 등록한 강아지의 프로필 이미지 URL. 이미지를 등록하지 않았으면 빈 문자열"),
								fieldWithPath("comments[].replyCount").description("삭제되지 않은 답글 수. 답글이 없으면 0"),
								fieldWithPath("comments[].createdAt").description("댓글 작성 시각(ISO-8601, 초 단위)"),
								fieldWithPath("nextCursor").optional()
										.description("다음 페이지 요청에 cursor로 보낼 값. 마지막 페이지면 null")
						)
				));
	}

	@Test
	void 댓글을_조회할_게시글이_없으면_404를_반환한다() throws Exception {
		given(commentFinder.findComments(eq(999L), any(CommentListRequest.class)))
				.willThrow(new PostNotFoundException(999L));

		mockMvc.perform(get("/api/community/posts/{postId}/comments", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_POST_NOT_FOUND"))
				.andDo(document("community/comment-list-error",
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
	void 댓글_목록의_커서와_size를_생략하면_첫_페이지를_기본_크기로_조회한다() throws Exception {
		given(commentFinder.findComments(eq(30L), any(CommentListRequest.class)))
				.willReturn(new CommentListResult(List.of(), null));

		mockMvc.perform(get("/api/community/posts/{postId}/comments", 30L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comments").isEmpty())
				.andExpect(jsonPath("$.nextCursor").value(nullValue()));

		then(commentFinder).should().findComments(30L, new CommentListRequest(null, CommentListRequest.DEFAULT_SIZE));
	}

	@Test
	void 댓글_목록의_size가_허용_범위를_벗어나면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/community/posts/{postId}/comments", 30L)
						.param("size", "51")
						.principal(CURRENT_USER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("size"));

		then(commentFinder).shouldHaveNoInteractions();
	}

	@Test
	void 답글_목록을_다음_커서와_함께_반환한다() throws Exception {
		Post post = PostFixture.create(30L, 7L, "이 레슨 따라해봤어요", LocalDateTime.of(2026, 10, 19, 10, 30, 0));
		ReplyResult reply = new ReplyResult(
				CommentFixture.create(102L, post, 7L, "감사합니다!", LocalDateTime.of(2026, 10, 19, 13, 0, 0)),
				new AuthorResult(7L, "초코", "https://media.meongcoach.com/images/dog-profile/7/choco.jpg"));
		given(commentFinder.findReplies(100L, new CommentListRequest(101L, 1)))
				.willReturn(new ReplyListResult(List.of(reply), 102L));

		mockMvc.perform(get("/api/community/comments/{commentId}/replies", 100L)
						.param("cursor", "101")
						.param("size", "1")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.replies[0].commentId").value(102))
				.andExpect(jsonPath("$.replies[0].content").value("감사합니다!"))
				.andExpect(jsonPath("$.replies[0].author.userId").value(7))
				.andExpect(jsonPath("$.replies[0].author.nickname").value("초코 보호자"))
				.andExpect(jsonPath("$.replies[0].createdAt").value("2026-10-19T13:00:00"))
				.andExpect(jsonPath("$.nextCursor").value(102))
				.andDo(document("community/reply-list",
						pathParameters(parameterWithName("commentId").description("답글을 조회할 루트 댓글 ID")),
						queryParameters(
								parameterWithName("cursor").optional()
										.description("이전 응답의 nextCursor. 첫 페이지는 생략하며, 이 ID보다 큰 답글부터 조회한다"),
								parameterWithName("size").optional().description("한 번에 조회할 답글 수. 1~50, 기본 20")
						),
						responseFields(
								fieldWithPath("replies[]").description("답글 목록. 오래된 순(댓글 ID 오름차순). 삭제된 답글은 담기지 않는다"),
								fieldWithPath("replies[].commentId").description("답글 ID"),
								fieldWithPath("replies[].content").description("답글 본문"),
								fieldWithPath("replies[].author").optional()
										.description("작성자 요약. 작성자가 탈퇴했거나 등록한 강아지가 없으면 null"),
								fieldWithPath("replies[].author.userId").description("작성자 회원 ID"),
								fieldWithPath("replies[].author.nickname").description("작성자 표시 이름. 작성자가 첫 번째로 등록한 강아지 이름 뒤에 ' 보호자'를 붙인다"),
								fieldWithPath("replies[].author.profileImageUrl").description("작성자가 첫 번째로 등록한 강아지의 프로필 이미지 URL. 이미지를 등록하지 않았으면 빈 문자열"),
								fieldWithPath("replies[].createdAt").description("답글 작성 시각(ISO-8601, 초 단위)"),
								fieldWithPath("nextCursor").optional()
										.description("다음 페이지 요청에 cursor로 보낼 값. 마지막 페이지면 null")
						)
				));
	}

	@Test
	void 답글을_조회할_루트_댓글이_없으면_404를_반환한다() throws Exception {
		given(commentFinder.findReplies(eq(999L), any(CommentListRequest.class)))
				.willThrow(new CommentNotFoundException(999L));

		mockMvc.perform(get("/api/community/comments/{commentId}/replies", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_NOT_FOUND"))
				.andDo(document("community/reply-list-error",
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
	void 게시글_미디어_업로드_URL을_발급한다() throws Exception {
		given(postMediaUploadUrlIssuer.issue(42L, new PostMediaUploadUrlRequest("VIDEO", "video/mp4", 10_485_760L)))
				.willReturn(new MediaUploadUrlResult(
						"https://account.r2.cloudflarestorage.com/bucket/videos/community-post/42/key.mp4?X-Amz-Signature=sig",
						"https://images.meongcoach.com/videos/community-post/42/key.mp4",
						"videos/community-post/42/key.mp4", 600L));

		mockMvc.perform(post("/api/community/presigned-urls")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"mediaType": "VIDEO", "contentType": "video/mp4", "fileSizeBytes": 10485760}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value(
						"https://account.r2.cloudflarestorage.com/bucket/videos/community-post/42/key.mp4?X-Amz-Signature=sig"))
				.andExpect(jsonPath("$.publicUrl").value("https://images.meongcoach.com/videos/community-post/42/key.mp4"))
				.andExpect(jsonPath("$.expiresInSeconds").value(600))
				.andDo(document("community/post-media-upload-url",
						requestFields(
								fieldWithPath("mediaType").description("미디어 종류. IMAGE(사진) 또는 VIDEO(영상)"),
								fieldWithPath("contentType")
										.description("파일 형식. 사진은 image/jpeg·image/png·image/webp, 영상은 video/mp4·video/quicktime"),
								fieldWithPath("fileSizeBytes")
										.description("업로드할 파일 크기(바이트, 양의 정수). 영상은 512MiB(536870912) 이하여야 하고 업로드 URL 서명에 포함된다. 사진은 상한이 없다")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"파일을 PUT할 presigned URL. 요청한 contentType을 Content-Type 헤더로 보내야 하고, 영상은 fileSizeBytes와 같은 크기로 올려야 한다"),
								fieldWithPath("publicUrl").description("업로드 후 파일이 공개되는 URL. 게시글 작성 요청에 이 값을 담는다"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	void 영상이_512MiB를_넘으면_400을_반환한다() throws Exception {
		PostMediaUploadUrlRequest request = new PostMediaUploadUrlRequest("VIDEO", "video/mp4", 536_870_913L);
		given(postMediaUploadUrlIssuer.issue(42L, request))
				.willThrow(new VideoFileSizeExceededException(536_870_913L, 536_870_912L));

		mockMvc.perform(post("/api/community/presigned-urls")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"mediaType": "VIDEO", "contentType": "video/mp4", "fileSizeBytes": 536870913}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_VIDEO_FILE_SIZE_EXCEEDED"))
				.andDo(document("community/post-media-upload-url-error",
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
	void 파일_크기가_없으면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/community/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"mediaType": "IMAGE", "contentType": "image/jpeg"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));

		then(postMediaUploadUrlIssuer).shouldHaveNoInteractions();
	}
}
