package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("select u.role from User u where u.id = :userId")
	Optional<UserRole> findRoleById(@Param("userId") Long userId);
}
