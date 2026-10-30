package com.daesabu.meongcoach.dashboard.adapter.webapi.dto;

import com.daesabu.meongcoach.shared.security.AuthorityRole;

public record DashboardMeResponse(Long userId, AuthorityRole userRole) {
}
