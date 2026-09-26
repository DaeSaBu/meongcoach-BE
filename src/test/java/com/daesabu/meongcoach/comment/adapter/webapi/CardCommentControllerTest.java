package com.daesabu.meongcoach.comment.adapter.webapi;

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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult;
import com.daesabu.meongcoach.comment.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.comment.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.comment.application.provided.CardCommentLiker;
import com.daesabu.meongcoach.comment.application.provided.CommentPageResult;
import com.daesabu.meongcoach.comment.application.provided.CommentResult;
import com.daesabu.meongcoach.comment.application.provided.LikeStateResult;
import com.daesabu.meongcoach.comment.application.provided.ReplyPageResult;
import com.daesabu.meongcoach.comment.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.comment.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.comment.domain.exception.InvalidCommentContentException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardCommentController.class)
@AutoConfigureRestDocs
class CardCommentControllerTest {

	private static final Long USER_ID = 42L;

	private static final Principal CURRENT_USER = () -> "42";

	private static final LocalDateTime CREATED_AT = LocalDateTime.parse("2026-09-25T02:10:00");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CardCommentFinder commentFinder;

	@MockitoBean
	private CardCommentCreator commentCreator;

	@MockitoBean
	private CardCommentLiker commentLiker;

	@Test
	void 카드의_최상위_댓글을_집계와_함께_한_페이지_조회한다() throws Exception {
		CommentResult mine = new CommentResult(12L, null, USER_ID, "멍멍이집사", "댓글", CREATED_AT, 1, 3, true);
		CommentResult others = new CommentResult(11L, null, 43L, "두부집사", "다른 댓글", CREATED_AT, 0, 0, false);
		given(commentFinder.findComments(USER_ID, 7L, 20L, 2)).willReturn(new CommentPageResult(List.of(mine, others), 5, 11L));

		mockMvc.perform(get("/api/training/cards/{cardId}/comments", 7L)
						.queryParam("cursor", "20").queryParam("size", "2")
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(5))
				.andExpect(jsonPath("$.nextCursor").value(11))
				.andExpect(jsonPath("$.comments[0].id").value(12))
				.andExpect(jsonPath("$.comments[0].mine").value(true))
				.andExpect(jsonPath("$.comments[0].replyCount").value(1))
				.andExpect(jsonPath("$.comments[0].likeCount").value(3))
				.andExpect(jsonPath("$.comments[0].likedByMe").value(true))
				.andExpect(jsonPath("$.comments[0].createdAt").value("2026-09-25T02:10:00"))
				.andExpect(jsonPath("$.comments[1].mine").value(false))
				.andDo(document("comment/list",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						queryParameters(
								parameterWithName("cursor").optional()
										.description("직전 페이지의 nextCursor. 첫 페이지는 생략"),
								parameterWithName("size").optional()
										.description("페이지 크기. 기본 20, 최대 50이며 범위를 벗어나면 경계값으로 조정")),
						responseFields(
								fieldWithPath("comments[]").description("최상위 댓글 목록. 최신순"),
								fieldWithPath("comments[].id").description("댓글 ID"),
								fieldWithPath("comments[].parentId").type(JsonFieldType.NUMBER).optional()
										.description("답한 대상 댓글 ID. 최상위 댓글은 null"),
								fieldWithPath("comments[].authorId").description("작성자 회원 ID"),
								fieldWithPath("comments[].authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("comments[].mine").description("내가 쓴 댓글인지 여부"),
								fieldWithPath("comments[].content").description("본문"),
								fieldWithPath("comments[].createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"),
								fieldWithPath("comments[].replyCount").description("답글 수"),
								fieldWithPath("comments[].likeCount").description("좋아요 수"),
								fieldWithPath("comments[].likedByMe").description("내 좋아요 여부"),
								fieldWithPath("totalCount").description("답글을 포함한 카드 전체 댓글 수"),
								fieldWithPath("nextCursor").type(JsonFieldType.NUMBER).optional()
										.description("다음 페이지 커서. 마지막 페이지면 null"))));
	}

	@Test
	void 댓글이_없으면_빈_목록과_0을_반환한다() throws Exception {
		given(commentFinder.findComments(USER_ID, 7L, null, 20)).willReturn(new CommentPageResult(List.of(), 0, null));

		mockMvc.perform(get("/api/training/cards/{cardId}/comments", 7L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comments").isEmpty())
				.andExpect(jsonPath("$.totalCount").value(0))
				.andExpect(jsonPath("$.nextCursor").value((Object) null));
	}

	@Test
	void 최상위_댓글의_답글을_오래된_순으로_한_페이지_조회한다() throws Exception {
		CommentResult reply = new CommentResult(15L, 12L, 43L, "두부집사", "답글", CREATED_AT, 0, 1, false);
		given(commentFinder.findReplies(USER_ID, 12L, null, 20)).willReturn(new ReplyPageResult(List.of(reply), null));

		mockMvc.perform(get("/api/training/comments/{commentId}/replies", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.replies[0].id").value(15))
				.andExpect(jsonPath("$.replies[0].parentId").value(12))
				.andExpect(jsonPath("$.replies[0].mine").value(false))
				.andExpect(jsonPath("$.replies[0].likeCount").value(1))
				.andExpect(jsonPath("$.nextCursor").value((Object) null))
				.andDo(document("comment/reply-list",
						pathParameters(parameterWithName("commentId").description("최상위 댓글 ID")),
						queryParameters(
								parameterWithName("cursor").optional()
										.description("직전 페이지의 nextCursor. 첫 페이지는 생략"),
								parameterWithName("size").optional()
										.description("페이지 크기. 기본 20, 최대 50이며 범위를 벗어나면 경계값으로 조정")),
						responseFields(
								fieldWithPath("replies[]").description("답글 목록. 오래된 순"),
								fieldWithPath("replies[].id").description("답글 ID"),
								fieldWithPath("replies[].parentId").description("답한 대상 댓글 ID. 최상위 댓글이거나 같은 스레드의 답글"),
								fieldWithPath("replies[].authorId").description("작성자 회원 ID"),
								fieldWithPath("replies[].authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("replies[].mine").description("내가 쓴 답글인지 여부"),
								fieldWithPath("replies[].content").description("본문"),
								fieldWithPath("replies[].createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"),
								fieldWithPath("replies[].replyCount").description("항상 0"),
								fieldWithPath("replies[].likeCount").description("좋아요 수"),
								fieldWithPath("replies[].likedByMe").description("내 좋아요 여부"),
								fieldWithPath("nextCursor").type(JsonFieldType.NUMBER).optional()
										.description("다음 페이지 커서. 마지막 페이지면 null"))));
	}

	@Test
	void 최상위_댓글이_아니면_답글_조회는_404를_반환한다() throws Exception {
		given(commentFinder.findReplies(USER_ID, 15L, null, 20)).willThrow(new CommentNotFoundException(15L));

		mockMvc.perform(get("/api/training/comments/{commentId}/replies", 15L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
				.andDo(document("comment/reply-list-error",
						pathParameters(parameterWithName("commentId").description("최상위 댓글 ID")),
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각. UTC"))));
	}

	@Test
	void 카드별_댓글_수를_일괄_조회한다() throws Exception {
		List<CardCommentCountResult> counts = List.of(
				new CardCommentCountResult(7L, 4),
				new CardCommentCountResult(8L, 1));
		given(commentFinder.countComments(Set.of(7L, 8L, 9L))).willReturn(counts);

		mockMvc.perform(get("/api/training/comments/counts")
						.queryParam("cardIds", "7", "8", "9")
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.counts[0].cardId").value(7))
				.andExpect(jsonPath("$.counts[0].totalCount").value(4))
				.andExpect(jsonPath("$.counts[1].cardId").value(8))
				.andDo(document("comment/counts",
						queryParameters(parameterWithName("cardIds").description("카드 ID 목록. 반복하거나 쉼표로 구분")),
						responseFields(
								fieldWithPath("counts[]").description("카드별 댓글 수. 댓글이 없는 카드는 없다"),
								fieldWithPath("counts[].cardId").description("카드 ID"),
								fieldWithPath("counts[].totalCount").description("답글을 포함한 카드 전체 댓글 수"))));
	}

	@Test
	void 댓글을_작성하면_201과_비어_있는_집계를_반환한다() throws Exception {
		CommentResult comment = new CommentResult(12L, null, USER_ID, "멍멍이집사", "댓글", CREATED_AT, 0, 0, false);
		given(commentCreator.createComment(USER_ID, 7L, new CommentCreateRequest("댓글"))).willReturn(comment);

		mockMvc.perform(post("/api/training/cards/{cardId}/comments", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.mine").value(true))
				.andExpect(jsonPath("$.authorNickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.replyCount").value(0))
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.likedByMe").value(false))
				.andDo(document("comment/create",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						requestFields(fieldWithPath("content").description("댓글 본문. 앞뒤 공백 제거 후 1~500자")),
						responseFields(
								fieldWithPath("id").description("댓글 ID"),
								fieldWithPath("parentId").type(JsonFieldType.NUMBER).optional()
										.description("답한 대상 댓글 ID. 최상위 댓글은 null"),
								fieldWithPath("authorId").description("작성자 회원 ID"),
								fieldWithPath("authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("mine").description("내가 쓴 댓글인지 여부. 작성 직후에는 true"),
								fieldWithPath("content").description("본문"),
								fieldWithPath("createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"),
								fieldWithPath("replyCount").description("답글 수. 작성 직후에는 0"),
								fieldWithPath("likeCount").description("좋아요 수. 작성 직후에는 0"),
								fieldWithPath("likedByMe").description("내 좋아요 여부. 작성 직후에는 false"))));
	}

	@Test
	void 잘못된_댓글_본문은_400과_에러_코드를_반환한다() throws Exception {
		given(commentCreator.createComment(USER_ID, 7L, new CommentCreateRequest("   ")))
				.willThrow(new InvalidCommentContentException());

		mockMvc.perform(post("/api/training/cards/{cardId}/comments", 7L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMENT_INVALID_CONTENT"))
				.andDo(document("comment/create-error",
						pathParameters(parameterWithName("cardId").description("카드 ID")),
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각. UTC"))));
	}

	@Test
	void 답글을_작성하면_201과_답한_대상을_반환한다() throws Exception {
		CommentResult reply = new CommentResult(15L, 12L, USER_ID, "멍멍이집사", "답글", CREATED_AT, 0, 0, false);
		given(commentCreator.createReply(USER_ID, 12L, new CommentCreateRequest("답글"))).willReturn(reply);

		mockMvc.perform(post("/api/training/comments/{commentId}/replies", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(15))
				.andExpect(jsonPath("$.parentId").value(12))
				.andExpect(jsonPath("$.mine").value(true))
				.andDo(document("comment/reply-create",
						pathParameters(parameterWithName("commentId").description("답할 댓글 ID. 답글이면 같은 스레드에 붙는다")),
						requestFields(fieldWithPath("content").description("답글 본문. 앞뒤 공백 제거 후 1~500자")),
						responseFields(
								fieldWithPath("id").description("답글 ID"),
								fieldWithPath("parentId").description("답한 대상 댓글 ID"),
								fieldWithPath("authorId").description("작성자 회원 ID"),
								fieldWithPath("authorNickname").description("작성자의 보호자 닉네임. 프로필이 없으면 null"),
								fieldWithPath("mine").description("내가 쓴 답글인지 여부. 작성 직후에는 true"),
								fieldWithPath("content").description("본문"),
								fieldWithPath("createdAt").description("작성 시각. 시간대 정보 없는 ISO-8601"),
								fieldWithPath("replyCount").description("항상 0"),
								fieldWithPath("likeCount").description("좋아요 수. 작성 직후에는 0"),
								fieldWithPath("likedByMe").description("내 좋아요 여부. 작성 직후에는 false"))));
	}

	@Test
	void 좋아요를_추가하고_취소한다() throws Exception {
		given(commentLiker.like(USER_ID, 12L)).willReturn(new LikeStateResult(4, true));
		given(commentLiker.unlike(USER_ID, 12L)).willReturn(new LikeStateResult(3, false));

		mockMvc.perform(post("/api/training/comments/{commentId}/like", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(4))
				.andExpect(jsonPath("$.likedByMe").value(true))
				.andDo(document("comment/like",
						pathParameters(parameterWithName("commentId").description("댓글 또는 답글 ID")),
						responseFields(
								fieldWithPath("likeCount").description("갱신된 좋아요 수"),
								fieldWithPath("likedByMe").description("내 좋아요 여부"))));

		mockMvc.perform(delete("/api/training/comments/{commentId}/like", 12L)
						.principal(CURRENT_USER).header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(3))
				.andExpect(jsonPath("$.likedByMe").value(false))
				.andDo(document("comment/unlike",
						pathParameters(parameterWithName("commentId").description("댓글 또는 답글 ID")),
						responseFields(
								fieldWithPath("likeCount").description("갱신된 좋아요 수"),
								fieldWithPath("likedByMe").description("내 좋아요 여부"))));
	}
}
