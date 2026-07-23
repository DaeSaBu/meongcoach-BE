package com.daesabu.meongcoach.health.adapter.webapi.dto;

public record HealthResponse(String status) {

	private static final String STATUS_UP = "UP";

	public static HealthResponse up() {
		return new HealthResponse(STATUS_UP);
	}
}
