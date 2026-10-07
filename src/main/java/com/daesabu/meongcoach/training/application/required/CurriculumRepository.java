package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
}
