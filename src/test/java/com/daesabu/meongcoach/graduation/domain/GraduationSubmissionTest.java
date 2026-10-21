package com.daesabu.meongcoach.graduation.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationReviewPendingException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
class GraduationSubmissionTest {
 private GraduationSubmission submission() {
  return GraduationSubmission.create(new GraduationSubmissionCommand(42L, 7L, "video", Instant.EPOCH));
 }
 @Test void 검수_대기_중_추가_제출을_거절한다() {
  assertThatThrownBy(() -> submission().validateCanSubmit()).isInstanceOf(GraduationReviewPendingException.class);
 }
 @Test void 승인_후_추가_제출과_재판정을_거절한다() {
  GraduationSubmission submission = submission();
  submission.approve(" 잘했어요 ", Instant.EPOCH);
  assertThat(submission.getReview().note()).isEqualTo("잘했어요");
  assertThat(submission.getReviewedAt()).isEqualTo(Instant.EPOCH);
  assertThatThrownBy(submission::validateCanSubmit).isInstanceOf(GraduationAlreadyApprovedException.class);
  assertThatThrownBy(() -> submission.approve(null, Instant.now())).isInstanceOf(IllegalStateException.class);
 }
 @Test void 보완_요청은_메모가_필수이고_재제출을_허용한다() {
  GraduationSubmission submission = submission();
  assertThatThrownBy(() -> submission.requestRevision(new GraduationRevisionCommand(" ", List.of(), null, Instant.EPOCH))).isInstanceOf(IllegalArgumentException.class);
  submission.requestRevision(new GraduationRevisionCommand(" 다시 촬영 ", List.of("전신"), "image", Instant.EPOCH));
  submission.validateCanSubmit();
  assertThat(submission.getReview()).isEqualTo(new GraduationReview("다시 촬영", List.of("전신")));
  assertThatThrownBy(() -> submission.getReview().retakeInstructions().add("추가")).isInstanceOf(UnsupportedOperationException.class);
 }
 @Test void 메모_없는_승인은_빈_판정_내용을_갖는다() {
  GraduationSubmission submission = submission();
  submission.approve(null, Instant.EPOCH);
  assertThat(submission.getReview()).isEqualTo(new GraduationReview(null, List.of()));
 }
}
