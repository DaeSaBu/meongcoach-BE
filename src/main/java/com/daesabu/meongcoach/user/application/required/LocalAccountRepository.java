package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.vo.Email;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

	// 로그인에서 회원 상태까지 함께 읽으므로 지연 로딩 대신 한 번에 조회한다
	@EntityGraph(attributePaths = "user")
	Optional<LocalAccount> findByEmail(Email email);

	void deleteByUser(User user);
}
