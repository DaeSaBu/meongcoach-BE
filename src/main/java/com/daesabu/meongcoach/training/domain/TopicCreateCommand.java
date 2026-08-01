package com.daesabu.meongcoach.training.domain;

public record TopicCreateCommand(String title, int sortOrder, String description, String detail, String iconUrl) {
}
