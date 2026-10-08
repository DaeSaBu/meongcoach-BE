package com.daesabu.meongcoach.promotion.application.required;

import com.daesabu.meongcoach.promotion.domain.PromotionCode;
import com.daesabu.meongcoach.promotion.domain.PromotionCodeRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionCodeRedemptionRepository extends JpaRepository<PromotionCodeRedemption, Long> {

	long countByPromotionCode(PromotionCode promotionCode);
}
