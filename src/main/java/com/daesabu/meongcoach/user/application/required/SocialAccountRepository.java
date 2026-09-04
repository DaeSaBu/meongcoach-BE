package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);

	Optional<SocialAccount> findByUserAndProvider(User user, SocialProvider provider);

	// 회원당 소셜 계정은 제공자 수만큼이라 파생 삭제(조회 후 건별 삭제)로 충분하다
	void deleteAllByUser(User user);
}
