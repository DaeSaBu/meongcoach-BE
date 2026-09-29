package com.daesabu.meongcoach.entitlement.application.required;

import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

	List<Entitlement> findAllByUserId(Long userId);
}
