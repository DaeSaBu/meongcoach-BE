package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
