package com.daesabu.meongcoach.graduation.application.provided.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;

import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;

public record GraduationSummaryResult( GraduationSubmission submission, TopicResult topic) {
}
