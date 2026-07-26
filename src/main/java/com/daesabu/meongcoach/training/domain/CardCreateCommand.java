package com.daesabu.meongcoach.training.domain;

public record CardCreateCommand(String title, int sortOrder, String instruction) {
}
