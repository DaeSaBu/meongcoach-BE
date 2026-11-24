package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.SelfReportNotAllowedException;
import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.application.required.ReportRepository;
import com.daesabu.meongcoach.report.domain.Report;
import com.daesabu.meongcoach.report.domain.ReportReason;
import com.daesabu.meongcoach.report.domain.exception.ReportAlreadyRegisteredException;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class ContentReportRegisterTest {

	private static final Long REPORTER_ID = 42L;

	private static final Long AUTHOR_ID = 41L;

	private static final ReportRegisterRequest REQUEST = new ReportRegisterRequest("ABUSE", "욕설이 있어요");

	@Autowired
	private ContentReportRegister contentReportRegister;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ReportRepository reportRepository;

	@Test
	void 게시글을_신고하면_게시글을_대상으로_한_신고를_저장한다() {
		Post post = savePost(AUTHOR_ID);

		contentReportRegister.reportPost(REPORTER_ID, post.getId(), REQUEST);

		List<Report> reports = reportRepository.findAll();
		assertThat(reports).hasSize(1);
		Report report = reports.getFirst();
		assertThat(report.getReporterId()).isEqualTo(REPORTER_ID);
		assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMUNITY_POST);
		assertThat(report.getTargetId()).isEqualTo(post.getId());
		assertThat(report.getReason()).isEqualTo(ReportReason.ABUSE);
		assertThat(report.getDetail()).isEqualTo("욕설이 있어요");
	}

	@Test
	void 없는_게시글은_신고할_수_없다() {
		assertThatThrownBy(() -> contentReportRegister.reportPost(REPORTER_ID, 999_999L, REQUEST))
				.isInstanceOf(PostNotFoundException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@Test
	void 삭제된_게시글은_신고할_수_없다() {
		Post post = savePost(AUTHOR_ID);
		post.delete();

		assertThatThrownBy(() -> contentReportRegister.reportPost(REPORTER_ID, post.getId(), REQUEST))
				.isInstanceOf(PostNotFoundException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@Test
	void 본인이_쓴_게시글은_신고할_수_없다() {
		Post post = savePost(REPORTER_ID);

		assertThatThrownBy(() -> contentReportRegister.reportPost(REPORTER_ID, post.getId(), REQUEST))
				.isInstanceOf(SelfReportNotAllowedException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@Test
	void 같은_게시글을_다시_신고하면_예외를_던진다() {
		Post post = savePost(AUTHOR_ID);
		contentReportRegister.reportPost(REPORTER_ID, post.getId(), REQUEST);

		assertThatThrownBy(() -> contentReportRegister.reportPost(REPORTER_ID, post.getId(), REQUEST))
				.isInstanceOf(ReportAlreadyRegisteredException.class);
		assertThat(reportRepository.findAll()).hasSize(1);
	}

	@Test
	void 댓글을_신고하면_댓글을_대상으로_한_신고를_저장한다() {
		Comment comment = saveComment(savePost(REPORTER_ID), AUTHOR_ID);

		contentReportRegister.reportComment(REPORTER_ID, comment.getId(), REQUEST);

		List<Report> reports = reportRepository.findAll();
		assertThat(reports).hasSize(1);
		Report report = reports.getFirst();
		assertThat(report.getReporterId()).isEqualTo(REPORTER_ID);
		assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMUNITY_COMMENT);
		assertThat(report.getTargetId()).isEqualTo(comment.getId());
		assertThat(report.getReason()).isEqualTo(ReportReason.ABUSE);
	}

	@Test
	void 삭제된_댓글은_신고할_수_없다() {
		Comment comment = saveComment(savePost(AUTHOR_ID), AUTHOR_ID);
		comment.delete();

		assertThatThrownBy(() -> contentReportRegister.reportComment(REPORTER_ID, comment.getId(), REQUEST))
				.isInstanceOf(CommentNotFoundException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@Test
	void 삭제된_게시글의_댓글은_신고할_수_없다() {
		Post post = savePost(AUTHOR_ID);
		Comment comment = saveComment(post, AUTHOR_ID);
		post.delete();

		assertThatThrownBy(() -> contentReportRegister.reportComment(REPORTER_ID, comment.getId(), REQUEST))
				.isInstanceOf(CommentNotFoundException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@Test
	void 본인이_쓴_댓글은_신고할_수_없다() {
		Comment comment = saveComment(savePost(AUTHOR_ID), REPORTER_ID);

		assertThatThrownBy(() -> contentReportRegister.reportComment(REPORTER_ID, comment.getId(), REQUEST))
				.isInstanceOf(SelfReportNotAllowedException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@Test
	void 같은_ID의_게시글과_댓글은_따로_신고할_수_있다() {
		Post post = savePost(AUTHOR_ID);
		Comment comment = saveComment(post, AUTHOR_ID);

		contentReportRegister.reportPost(REPORTER_ID, post.getId(), REQUEST);
		contentReportRegister.reportComment(REPORTER_ID, comment.getId(), REQUEST);

		assertThat(reportRepository.findAll())
				.extracting(Report::getTargetType)
				.containsExactlyInAnyOrder(ReportTargetType.COMMUNITY_POST, ReportTargetType.COMMUNITY_COMMENT);
	}

	private Post savePost(Long authorId) {
		return postRepository.save(Post.register(authorId, new PostRegisterCommand("오늘 앉아 훈련 성공!", null, List.of())));
	}

	private Comment saveComment(Post post, Long authorId) {
		return commentRepository.save(Comment.register(post, authorId, "댓글"));
	}
}
