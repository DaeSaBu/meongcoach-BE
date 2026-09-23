package com.daesabu.meongcoach.auth.application.required;

import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.EmailAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long> {

	Optional<EmailAccount> findByEmail(Email email);

	void deleteAllByUserId(Long userId);
}
