package com.daesabu.meongcoach.dog.application.required;

import com.daesabu.meongcoach.dog.domain.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DogRepository extends JpaRepository<Dog, Long> {
}
