package com.daesabu.meongcoach.health.adapter.webapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daesabu.meongcoach.health.adapter.webapi.dto.HealthResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public HealthResponse check() {
		return HealthResponse.up();
	}
}
