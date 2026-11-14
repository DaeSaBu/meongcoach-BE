package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.UserStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("select u.role from User u where u.id = :userId and u.status <> :excludedStatus")
	Optional<UserRole> findRoleByIdAndStatusNot(@Param("userId") Long userId,
	                                            @Param("excludedStatus") UserStatus excludedStatus);

	@Query("select u.id from User u where u.role in :roles and u.status = :status")
	Set<Long> findIdsByRoleInAndStatus(@Param("roles") Collection<UserRole> roles, @Param("status") UserStatus status);

	@Query("select u.id from User u where u.id in :userIds and u.status = :status")
	Set<Long> findIdsByIdInAndStatus(@Param("userIds") Collection<Long> userIds, @Param("status") UserStatus status);
}
