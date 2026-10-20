package com.daesabu.meongcoach.community.adapter.persistence;

import static com.daesabu.meongcoach.community.domain.QPost.post;

import com.daesabu.meongcoach.community.application.required.PostQueryRepository;
import com.daesabu.meongcoach.community.domain.Post;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuerydslPostQueryRepository implements PostQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Slice<Post> findPosts(Long cursor, int size) {
		List<Post> posts = queryFactory.selectFrom(post)
				.where(idLessThan(cursor))
				.orderBy(post.id.desc())
				.limit(size + 1)
				.fetch();
		return toSlice(posts, size);
	}

	private BooleanExpression idLessThan(Long cursor) {
		if (cursor == null) {
			return null;
		}
		return post.id.lt(cursor);
	}

	private Slice<Post> toSlice(List<Post> posts, int size) {
		boolean hasNext = posts.size() > size;
		List<Post> content = posts.stream()
				.limit(size)
				.toList();
		return new SliceImpl<>(content, PageRequest.of(0, size), hasNext);
	}
}
