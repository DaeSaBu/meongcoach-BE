package com.daesabu.meongcoach.purchase.application.provided;

import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import jakarta.validation.Valid;

public interface PurchaseRegister {
	void register(@Valid PurchaseRegisterRequest purchaseRegisterRequest);
}
