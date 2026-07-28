package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
