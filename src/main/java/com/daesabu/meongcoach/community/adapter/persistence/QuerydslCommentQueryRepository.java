package com.daesabu.meongcoach.community.adapter.persistence;

import static com.daesabu.meongcoach.community.domain.QComment.comment;
import static com.daesabu.meongcoach.community.domain.QCommentClosure.commentClosure;

import com.daesabu.meongcoach.community.application.required.CommentQueryRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuerydslCommentQueryRepository implements CommentQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Slice<Comment> findRootComments(Long postId, Long cursor, int size) {
		List<Comment> comments = queryFactory.selectFrom(comment)
				.where(comment.post.id.eq(postId), idGreaterThan(cursor), isRoot(), comment.deletedAt.isNull())
				.orderBy(comment.id.asc())
				.limit(size + 1)
				.fetch();
		return toSlice(comments, size);
	}

	private BooleanExpression idGreaterThan(Long cursor) {
		if (cursor == null) {
			return null;
		}
		return comment.id.gt(cursor);
	}

	private BooleanExpression isRoot() {
		return JPAExpressions.selectOne()
				.from(commentClosure)
				.where(commentClosure.descendant.eq(comment), commentClosure.depth.eq(CommentClosure.REPLY_DEPTH))
				.notExists();
	}

	private Slice<Comment> toSlice(List<Comment> comments, int size) {
		boolean hasNext = comments.size() > size;
		List<Comment> content = comments.stream()
				.limit(size)
				.toList();
		return new SliceImpl<>(content, PageRequest.of(0, size), hasNext);
	}
}
