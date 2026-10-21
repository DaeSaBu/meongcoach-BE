package com.daesabu.meongcoach.graduation.domain;
import java.util.List;
public record GraduationReview(String note, List<String> retakeInstructions) {
 public GraduationReview { retakeInstructions = List.copyOf(retakeInstructions); }
}
