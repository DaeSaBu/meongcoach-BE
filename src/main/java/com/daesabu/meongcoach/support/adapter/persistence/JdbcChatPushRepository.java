package com.daesabu.meongcoach.support.adapter.persistence;

import com.daesabu.meongcoach.support.application.required.ChatPushRepository;
import com.daesabu.meongcoach.support.domain.ChatPushJob;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcChatPushRepository implements ChatPushRepository {
	private final JdbcTemplate jdbc;

	public void enqueue(long messageId, long userId) {
		jdbc.update(
				"INSERT INTO support_push_jobs(message_id,user_id) VALUES (?,?) ON CONFLICT DO"
					+ " NOTHING",
				messageId,
				userId);
	}

	public Optional<ChatPushJob> claim() {
		jdbc.update(
				"""
				WITH expired AS (
				 SELECT message_id FROM support_push_jobs WHERE state='PROCESSING' AND attempts>=8
				 AND lease_until<=clock_timestamp() ORDER BY message_id FOR UPDATE SKIP LOCKED LIMIT 100
				) UPDATE support_push_jobs j SET state='DEAD',lease_token=NULL,lease_until=NULL,
				last_error='LEASE_EXPIRED_MAX_ATTEMPTS',finished_at=clock_timestamp()
				FROM expired WHERE j.message_id=expired.message_id
				""");
		UUID token = UUID.randomUUID();
		return jdbc
				.query(
						"""
						WITH candidate AS (
						 SELECT message_id FROM support_push_jobs WHERE attempts<8 AND
						 ((state='PENDING' AND available_at<=clock_timestamp()) OR
						  (state='PROCESSING' AND lease_until<=clock_timestamp()))
						 ORDER BY available_at,message_id FOR UPDATE SKIP LOCKED LIMIT 1
						) UPDATE support_push_jobs j SET state='PROCESSING',attempts=attempts+1,
						lease_token=?,lease_until=clock_timestamp()+interval '10 minutes'
						FROM candidate WHERE j.message_id=candidate.message_id
						RETURNING j.message_id,j.user_id,j.attempts,j.lease_token
						""",
						(rs, n) ->
								new ChatPushJob(
										rs.getLong("message_id"),
										rs.getLong("user_id"),
										rs.getInt("attempts"),
										rs.getObject("lease_token", UUID.class)),
						token)
				.stream()
				.findFirst();
	}

	public boolean complete(ChatPushJob job) {
		int count =
				jdbc.update(
						"""
						UPDATE support_push_jobs SET state='SENT',finished_at=clock_timestamp(),lease_token=NULL,
						lease_until=NULL,last_error=NULL WHERE message_id=? AND state='PROCESSING' AND lease_token=?
						AND lease_until>clock_timestamp()
						""",
						job.messageId(),
						job.leaseToken());
		return count == 1;
	}

	public boolean fail(ChatPushJob job, String error) {
		String state = "PENDING";
		if (job.attempts() >= 8) {
			state = "DEAD";
		}
		long delay = Math.min(3600, 30L << (job.attempts() - 1));
		int count =
				jdbc.update(
						"""
						UPDATE support_push_jobs SET state=?,available_at=clock_timestamp()+(? * interval '1 second'),
						finished_at=CASE WHEN ?='DEAD' THEN clock_timestamp() ELSE NULL END,
						lease_token=NULL,lease_until=NULL,last_error=?
						WHERE message_id=? AND state='PROCESSING' AND lease_token=? AND lease_until>clock_timestamp()
						""",
						state,
						delay,
						state,
						error,
						job.messageId(),
						job.leaseToken());
		return count == 1;
	}
}
