package com.daesabu.meongcoach.training.domain;

public record CardMediaCreateCommand(MediaType mediaType, String url, int sortOrder) {
}
