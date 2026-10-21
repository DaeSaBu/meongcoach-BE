package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionCommand;
import com.daesabu.meongcoach.graduation.domain.GraduationVideoKey;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationSubmitService implements GraduationSubmitter {

	private final TopicCompletionChecker topicCompletionChecker;
	private final GraduationSubmissionRepository graduationSubmissionRepository;

 @Override
 @Transactional
 public GraduationSubmission submit(Long userId, Long topicId, GraduationSubmissionRequest request) {
  GraduationVideoKey videoKey = GraduationVideoKey.of(request.objectKey(), userId, topicId);
  graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(userId, topicId)
   .ifPresent(GraduationSubmission::validateCanSubmit);
  if (!topicCompletionChecker.isCompleted(userId, topicId)) {
   throw new TopicNotCompletedException(topicId);
  }
  GraduationSubmission submission = GraduationSubmission.create(
   new GraduationSubmissionCommand(userId, topicId, videoKey.value(), Instant.now()));
  try {
   return graduationSubmissionRepository.saveAndFlush(submission);
  } catch (DataIntegrityViolationException e) {
   throw new GraduationSubmissionConflictException(topicId, e);
  }
 }
}
