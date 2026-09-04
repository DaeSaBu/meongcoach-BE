package com.daesabu.meongcoach.training.domain;

public record LessonCreateCommand(String title, int sortOrder, Integer estimatedMinutes) {
}
