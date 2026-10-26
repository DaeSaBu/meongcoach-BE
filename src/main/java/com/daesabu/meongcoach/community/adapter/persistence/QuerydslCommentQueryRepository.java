package com.daesabu.meongcoach.community.adapter.persistence;

import static com.daesabu.meongcoach.community.domain.QComment.comment;
import static com.daesabu.meongcoach.community.domain.QCommentClosure.commentClosure;
import static java.util.stream.Collectors.toMap;

import com.daesabu.meongcoach.community.application.required.CommentQueryRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.daesabu.meongcoach.community.domain.QComment;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuerydslCommentQueryRepository implements CommentQueryRepository {

	private static final QComment reply = new QComment("reply");
	private static final QComment root = new QComment("root");

	private final JPAQueryFactory queryFactory;

	@Override
	public Slice<Comment> findRootComments(Long postId, Long cursor, int size) {
		List<Comment> comments = queryFactory.selectFrom(comment)
				.where(comment.post.id.eq(postId), idGreaterThan(comment, cursor), isRoot(), comment.deletedAt.isNull())
				.orderBy(comment.id.asc())
				.limit(size + 1)
				.fetch();
		return toSlice(comments, size);
	}

	@Override
	public Map<Long, Long> countReplies(Collection<Long> rootIds) {
		if (rootIds.isEmpty()) {
			return Map.of();
		}
		NumberExpression<Long> replyCount = commentClosure.count();
		List<Tuple> rows = queryFactory.select(commentClosure.ancestor.id, replyCount)
				.from(commentClosure)
				.join(commentClosure.descendant, reply)
				.where(commentClosure.ancestor.id.in(rootIds), commentClosure.depth.eq(CommentClosure.REPLY_DEPTH),
						reply.deletedAt.isNull())
				.groupBy(commentClosure.ancestor.id)
				.fetch();
		return rows.stream()
				.collect(toMap(row -> row.get(commentClosure.ancestor.id), row -> row.get(replyCount)));
	}

	@Override
	public Map<Long, Long> countComments(Collection<Long> postIds) {
		if (postIds.isEmpty()) {
			return Map.of();
		}
		NumberExpression<Long> commentCount = comment.count();
		List<Tuple> rows = queryFactory.select(comment.post.id, commentCount)
				.from(comment)
				.where(comment.post.id.in(postIds), comment.deletedAt.isNull(), hasNoDeletedRoot())
				.groupBy(comment.post.id)
				.fetch();
		return rows.stream()
				.collect(toMap(row -> row.get(comment.post.id), row -> row.get(commentCount)));
	}

	@Override
	public Slice<Comment> findReplies(Long rootId, Long cursor, int size) {
		List<Comment> replies = queryFactory.select(reply)
				.from(commentClosure)
				.join(commentClosure.descendant, reply)
				.where(commentClosure.ancestor.id.eq(rootId), commentClosure.depth.eq(CommentClosure.REPLY_DEPTH),
						reply.deletedAt.isNull(), idGreaterThan(reply, cursor))
				.orderBy(reply.id.asc())
				.limit(size + 1)
				.fetch();
		return toSlice(replies, size);
	}

	private BooleanExpression idGreaterThan(QComment target, Long cursor) {
		if (cursor == null) {
			return null;
		}
		return target.id.gt(cursor);
	}

	private BooleanExpression isRoot() {
		return JPAExpressions.selectOne()
				.from(commentClosure)
				.where(commentClosure.descendant.eq(comment), commentClosure.depth.eq(CommentClosure.REPLY_DEPTH))
				.notExists();
	}

	private BooleanExpression hasNoDeletedRoot() {
		return JPAExpressions.selectOne()
				.from(commentClosure)
				.join(commentClosure.ancestor, root)
				.where(commentClosure.descendant.eq(comment), commentClosure.depth.eq(CommentClosure.REPLY_DEPTH),
						root.deletedAt.isNotNull())
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
