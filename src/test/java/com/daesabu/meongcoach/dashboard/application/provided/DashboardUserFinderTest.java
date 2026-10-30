package com.daesabu.meongcoach.dashboard.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dashboard.domain.exception.DashboardUserNotFoundException;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@ApplicationTest
class DashboardUserFinderTest {

	private static final long UNREGISTERED_USER_ID = 1_000_000L;

	@Autowired
	private DashboardUserFinder dashboardUserFinder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void 회원의_역할을_반환한다() {
		Long userId = userRepository.save(User.registerUser()).getId();
		jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", "ADMIN", userId);

		assertThat(dashboardUserFinder.findRole(userId)).isEqualTo(AuthorityRole.ADMIN);
	}

	@Test
	void 없는_회원이면_예외를_던진다() {
		assertThatThrownBy(() -> dashboardUserFinder.findRole(UNREGISTERED_USER_ID))
				.isInstanceOf(DashboardUserNotFoundException.class);
	}
}
