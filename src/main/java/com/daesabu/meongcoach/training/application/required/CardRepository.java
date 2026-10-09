package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}
