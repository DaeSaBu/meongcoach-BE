package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CommentTest {

	private static final Long USER_ID = 42L;
	private static final String CONTENT = "저희 강아지도 성공했어요!";
	private static final String IMAGE_URL = "https://media.meongcoach.com/images/community-post/42/shot.jpg";
	private static final String VIDEO_URL = "https://media.meongcoach.com/videos/community-post/42/clip.mp4";

	private final Post post = Post.register(7L, new PostRegisterCommand("오늘 앉아 훈련 성공!", null, List.of()));

	@Test
	void 등록하면_게시글과_작성자와_본문이_채워진다() {
		Comment comment = Comment.register(post, USER_ID, "저희 강아지도 성공했어요!");

		assertThat(comment.getPost()).isSameAs(post);
		assertThat(comment.getUserId()).isEqualTo(USER_ID);
		assertThat(comment.getContent()).isEqualTo(new CommentContent("저희 강아지도 성공했어요!"));
		assertThat(comment.getDeletedAt()).isNull();
	}

	@Test
	void 본문이_비어_있으면_등록할_수_없다() {
		assertThatThrownBy(() -> Comment.register(post, USER_ID, "   "))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 삭제하면_삭제_시각이_기록된다() {
		Comment comment = Comment.register(post, USER_ID, "저희 강아지도 성공했어요!");

		comment.delete();

		assertThat(comment.getDeletedAt()).isNotNull();
	}

	@Test
	void 수정하면_본문이_교체된다() {
		Comment comment = Comment.register(post, USER_ID, "저희 강아지도 성공했어요!");

		comment.update("수정한 댓글");

		assertThat(comment.getContent()).isEqualTo(new CommentContent("수정한 댓글"));
	}

	@Test
	void 본문이_비어_있으면_수정할_수_없다() {
		Comment comment = Comment.register(post, USER_ID, "저희 강아지도 성공했어요!");

		assertThatThrownBy(() -> comment.update("   "))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 첨부_없이_등록하면_첨부가_비어_있다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT);

		assertThat(comment.getMedia()).isEmpty();
	}

	@Test
	void 첨부와_함께_등록하면_요청_순서대로_첨부가_채워진다() {
		List<PostMediaCommand> media = List.of(
				new PostMediaCommand(PostMediaType.VIDEO, VIDEO_URL),
				new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL));

		Comment comment = Comment.register(post, USER_ID, CONTENT, media);

		assertThat(comment.getMedia())
				.extracting(CommentMedia::getType, CommentMedia::getUrl, CommentMedia::getSortOrder)
				.containsExactly(
						tuple(PostMediaType.VIDEO, VIDEO_URL, 0),
						tuple(PostMediaType.IMAGE, IMAGE_URL, 1));
	}

	@Test
	void 사진은_3개까지_첨부할_수_있다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT, images(3));

		assertThat(comment.getMedia()).hasSize(3);
	}

	@Test
	void 사진이_3개를_넘으면_등록할_수_없다() {
		assertThatThrownBy(() -> Comment.register(post, USER_ID, CONTENT, images(4)))
				.isInstanceOf(PostMediaCountExceededException.class);
	}

	@Test
	void 영상이_2개를_넘으면_등록할_수_없다() {
		assertThatThrownBy(() -> Comment.register(post, USER_ID, CONTENT, videos(3)))
				.isInstanceOf(PostMediaCountExceededException.class);
	}

	@Test
	void 첨부가_있어도_본문이_비어_있으면_등록할_수_없다() {
		assertThatThrownBy(() -> Comment.register(post, USER_ID, " ", images(1)))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 첨부를_넘겨_수정하면_첨부가_교체된다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT, images(2));

		comment.update("수정한 댓글", List.of(new PostMediaCommand(PostMediaType.VIDEO, VIDEO_URL)));

		assertThat(comment.getMedia())
				.extracting(CommentMedia::getType, CommentMedia::getUrl, CommentMedia::getSortOrder)
				.containsExactly(tuple(PostMediaType.VIDEO, VIDEO_URL, 0));
	}

	@Test
	void 빈_첨부로_수정하면_첨부가_지워진다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT, images(2));

		comment.update("수정한 댓글", List.of());

		assertThat(comment.getMedia()).isEmpty();
	}

	@Test
	void 첨부를_생략하고_수정하면_기존_첨부를_유지한다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT, images(2));

		comment.update("수정한 댓글", null);

		assertThat(comment.getContent()).isEqualTo(new CommentContent("수정한 댓글"));
		assertThat(comment.getMedia()).hasSize(2);
	}

	@Test
	void 수정할_첨부가_한도를_넘으면_수정할_수_없다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT);

		assertThatThrownBy(() -> comment.update("수정한 댓글", images(4)))
				.isInstanceOf(PostMediaCountExceededException.class);
	}

	@Test
	void 첨부_목록은_밖에서_바꿀_수_없다() {
		Comment comment = Comment.register(post, USER_ID, CONTENT, images(1));

		assertThatThrownBy(() -> comment.getMedia().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private List<PostMediaCommand> images(int count) {
		return IntStream.range(0, count)
				.mapToObj(index -> new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL))
				.toList();
	}

	private List<PostMediaCommand> videos(int count) {
		return IntStream.range(0, count)
				.mapToObj(index -> new PostMediaCommand(PostMediaType.VIDEO, VIDEO_URL))
				.toList();
	}
}
