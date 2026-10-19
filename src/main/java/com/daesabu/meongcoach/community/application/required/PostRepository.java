package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

	Slice<Post> findAllByOrderByIdDesc(Pageable pageable);

	Slice<Post> findAllByIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}
