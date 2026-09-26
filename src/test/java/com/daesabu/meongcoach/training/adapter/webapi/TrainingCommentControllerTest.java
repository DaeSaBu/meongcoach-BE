package com.daesabu.meongcoach.training.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.training.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeUpdater;
import com.daesabu.meongcoach.training.application.provided.CommentLikeResult;
import com.daesabu.meongcoach.training.application.provided.CommentResult;
import com.daesabu.meongcoach.training.application.provided.LikeStateResult;
import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import com.daesabu.meongcoach.training.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.training.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.InvalidCardCommentContentException;
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

@WebMvcTest(TrainingCommentController.class)
@AutoConfigureRestDocs
class TrainingCommentControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CardCommentFinder commentFinder;

	@MockitoBean
	private CardCommentCreator commentCreator;

	@MockitoBean
	private CardCommentLikeUpdater likeUpdater;

	@MockitoBean
	private CardCommentLikeFinder likeFinder;

	@Test
	void 카드의_부모_댓글과_답글_수를_조회한다() throws Exception {
		CommentResult comment = new CommentResult(12L, "멍멍이집사", "댓글", LocalDateTime.parse("2026-09-25T02:10:00"), 1);
		given(commentFinder.findComments(7L)).willReturn(List.of(comment));

		mockMvc.perform(get("/api/training/cards/{cardId}/comments", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").doesNotExist())
				.andExpect(jsonPath("$.comments[0].authorNickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.comments[0].replyCount").value(1))
				.andExpect(jsonPath("$.comments[0].likeCount").doesNotExist())
				.andExpect(jsonPath("$.comments[0].replies").doesNotExist())
				.andExpect(jsonPath("$.comments[0].createdAt").value("2026-09-25T02:10:00"))
				.andExpect(jsonPath("$.comments[0].mine").doesNotExist())
				.andExpect(jsonPath("$.nextCursor").doesNotExist())
				.andExpect(jsonPath("$.hasNext").doesNotExist())
				.andDo(document("training/comment-list",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						responseFields(
								fieldWithPath("comments[]").description("최상위 댓글 목록"),
								fieldWithPath("comments[].id").description("댓글 ID"),
								fieldWithPath("comments[].authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("comments[].content").description("본문"),
								fieldWithPath("comments[].createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"),
								fieldWithPath("comments[].replyCount").description("답글 수"))));
	}

	@Test
	void 카드의_전체_댓글_수를_별도로_조회한다() throws Exception {
		given(commentFinder.countComments(7L)).willReturn(4L);

		mockMvc.perform(get("/api/training/cards/{cardId}/comments/count", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(4))
				.andDo(document("training/comment-count",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						responseFields(fieldWithPath("totalCount").description("답글을 포함한 카드 전체 댓글 수"))));
	}

	@Test
	void 부모_댓글의_답글을_별도로_조회한다() throws Exception {
		ReplyResult reply = new ReplyResult(15L, "두부집사", "답글", LocalDateTime.parse("2026-09-25T02:12:00"));
		given(commentFinder.findReplies(12L)).willReturn(List.of(reply));

		mockMvc.perform(get("/api/training/comments/{parentId}/replies", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.replies[0].id").value(15))
				.andExpect(jsonPath("$.replies[0].authorNickname").value("두부집사"))
				.andExpect(jsonPath("$.replies[0].createdAt").value("2026-09-25T02:12:00"))
				.andExpect(jsonPath("$.replies[0].likeCount").doesNotExist())
				.andDo(document("training/comment-reply-list",
						pathParameters(parameterWithName("parentId").description("부모 댓글 ID")),
						responseFields(
								fieldWithPath("replies[]").description("오래된 순으로 정렬된 답글"),
								fieldWithPath("replies[].id").description("답글 ID"),
								fieldWithPath("replies[].authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("replies[].content").description("본문"),
								fieldWithPath("replies[].createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"))));
	}

	@Test
	void 부모_댓글이_없으면_답글_조회는_404를_반환한다() throws Exception {
		given(commentFinder.findReplies(12L)).willThrow(new CommentNotFoundException(12L));

		mockMvc.perform(get("/api/training/comments/{parentId}/replies", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_COMMENT_NOT_FOUND"))
				.andDo(document("training/comment-reply-list-error",
						pathParameters(parameterWithName("parentId").description("부모 댓글 ID")),
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각. UTC"))));
	}

	@Test
	void 댓글이_없으면_빈_목록을_반환한다() throws Exception {
		given(commentFinder.findComments(7L)).willReturn(List.of());

		mockMvc.perform(get("/api/training/cards/{cardId}/comments", 7L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").doesNotExist())
				.andExpect(jsonPath("$.comments").isEmpty());
	}

	@Test
	void 댓글을_작성하면_201과_답글_수_0을_반환한다() throws Exception {
		CommentResult comment = new CommentResult(12L, "멍멍이집사", "댓글", LocalDateTime.parse("2026-09-25T02:10:00"), 0);
		given(commentCreator.createComment(42L, 7L, new CommentCreateRequest("댓글")))
				.willReturn(comment);

		mockMvc.perform(post("/api/training/cards/{cardId}/comments", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.authorNickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.createdAt").value("2026-09-25T02:10:00"))
				.andExpect(jsonPath("$.mine").doesNotExist())
				.andExpect(jsonPath("$.replyCount").value(0))
				.andExpect(jsonPath("$.likeCount").doesNotExist())
				.andExpect(jsonPath("$.replies").doesNotExist())
				.andDo(document("training/comment-create",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						requestFields(fieldWithPath("content").description("댓글 본문. 앞뒤 공백 제거 후 1~500자")),
						responseFields(
								fieldWithPath("id").description("댓글 ID"),
								fieldWithPath("authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("content").description("본문"),
								fieldWithPath("createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"),
								fieldWithPath("replyCount").description("답글 수. 생성 직후에는 0"))));
	}

	@Test
	void 답글을_작성하면_201을_반환하고_replies_필드가_없다() throws Exception {
		ReplyResult reply = new ReplyResult(15L, "두부집사", "답글", LocalDateTime.parse("2026-09-25T02:12:00"));
		given(commentCreator.createReply(42L, 12L, new CommentCreateRequest("답글")))
				.willReturn(reply);

		mockMvc.perform(post("/api/training/comments/{commentId}/replies", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(15))
				.andExpect(jsonPath("$.authorNickname").value("두부집사"))
				.andExpect(jsonPath("$.createdAt").value("2026-09-25T02:12:00"))
				.andExpect(jsonPath("$.mine").doesNotExist())
				.andExpect(jsonPath("$.replies").doesNotExist())
				.andExpect(jsonPath("$.likeCount").doesNotExist())
				.andDo(document("training/comment-reply-create",
						pathParameters(parameterWithName("commentId").description("부모 댓글 또는 답글 ID")),
						requestFields(fieldWithPath("content").description("답글 본문. 앞뒤 공백 제거 후 1~500자")),
						responseFields(
								fieldWithPath("id").description("답글 ID"),
								fieldWithPath("authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("content").description("본문"),
								fieldWithPath("createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"))));
	}

	@Test
	void 카드의_댓글과_답글_좋아요_상태를_조회한다() throws Exception {
		List<CommentLikeResult> likes = List.of(
				new CommentLikeResult(12L, 3, true),
				new CommentLikeResult(15L, 1, false));
		given(likeFinder.findLikeStates(42L, 7L)).willReturn(likes);

		mockMvc.perform(get("/api/training/cards/{cardId}/comments/likes", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likes[0].commentId").value(12))
				.andExpect(jsonPath("$.likes[0].likeCount").value(3))
				.andExpect(jsonPath("$.likes[0].likedByMe").value(true))
				.andExpect(jsonPath("$.likes[1].commentId").value(15))
				.andExpect(jsonPath("$.likes[1].likedByMe").value(false))
				.andDo(document("training/comment-likes",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						responseFields(
								fieldWithPath("likes[]").description("카드에서 좋아요가 있는 댓글·답글의 상태. 없는 항목은 좋아요 0건이며 내 좋아요도 없음"),
								fieldWithPath("likes[].commentId").description("댓글 또는 답글 ID"),
								fieldWithPath("likes[].likeCount").description("좋아요 수"),
								fieldWithPath("likes[].likedByMe").description("내 좋아요 여부"))));
	}

	@Test
	void 좋아요를_추가하고_취소한다() throws Exception {
		given(likeUpdater.like(42L, 12L)).willReturn(new LikeStateResult(4, true));
		given(likeUpdater.unlike(42L, 12L)).willReturn(new LikeStateResult(3, false));

		mockMvc.perform(post("/api/training/comments/{commentId}/like", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(4))
				.andExpect(jsonPath("$.likedByMe").value(true))
				.andDo(document("training/comment-like",
						pathParameters(parameterWithName("commentId").description("댓글 또는 답글 ID")),
						responseFields(
								fieldWithPath("likeCount").description("갱신된 좋아요 수"),
								fieldWithPath("likedByMe").description("내 좋아요 여부"))));

		mockMvc.perform(delete("/api/training/comments/{commentId}/like", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(3))
				.andExpect(jsonPath("$.likedByMe").value(false))
				.andDo(document("training/comment-unlike",
						pathParameters(parameterWithName("commentId").description("댓글 또는 답글 ID")),
						responseFields(
								fieldWithPath("likeCount").description("갱신된 좋아요 수"),
								fieldWithPath("likedByMe").description("내 좋아요 여부"))));
	}

	@Test
	void 잘못된_댓글_본문은_400과_에러_코드를_반환한다() throws Exception {
		given(commentCreator.createComment(42L, 7L, new CommentCreateRequest("   ")))
				.willThrow(new InvalidCardCommentContentException());

		mockMvc.perform(post("/api/training/cards/{cardId}/comments", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TRAINING_COMMENT_INVALID_CONTENT"))
				.andDo(document("training/comment-create-error",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각. UTC"))));
	}
}
