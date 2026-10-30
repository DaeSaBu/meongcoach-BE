package com.daesabu.meongcoach.dashboard.adapter.webapi;

import com.daesabu.meongcoach.dashboard.adapter.webapi.dto.DashboardMeResponse;
import com.daesabu.meongcoach.dashboard.application.provided.DashboardUserFinder;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardUserFinder dashboardUserFinder;

	@GetMapping("/me")
	public DashboardMeResponse me(@CurrentUserId Long userId) {
		AuthorityRole role = dashboardUserFinder.findRole(userId);
		return new DashboardMeResponse(userId, role);
	}
}
