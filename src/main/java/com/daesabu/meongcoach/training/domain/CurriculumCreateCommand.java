package com.daesabu.meongcoach.training.domain;

public record CurriculumCreateCommand(String title, int sortOrder, String thumbnailUrl, String description) {
}
