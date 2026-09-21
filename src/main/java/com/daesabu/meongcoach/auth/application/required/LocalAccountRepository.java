package com.daesabu.meongcoach.auth.application.required;

import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.LocalAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

	Optional<LocalAccount> findByEmail(Email email);

	void deleteByUserId(Long userId);
}
