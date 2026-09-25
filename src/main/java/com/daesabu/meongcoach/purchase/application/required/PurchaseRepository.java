package com.daesabu.meongcoach.purchase.application.required;

import com.daesabu.meongcoach.purchase.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
	boolean existsByTransactionId(String transactionId);
}
