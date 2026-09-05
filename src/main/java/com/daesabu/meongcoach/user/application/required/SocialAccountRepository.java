package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);

	// 회원당 소셜 계정은 제공자 수만큼이라 탈퇴 시 조회한 목록을 그대로 revoke·삭제에 쓴다
	List<SocialAccount> findAllByUser(User user);
}
