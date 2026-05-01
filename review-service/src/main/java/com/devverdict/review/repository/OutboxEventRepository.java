package com.devverdict.review.repository;

import com.devverdict.review.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByPublishedAtIsNullAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetries);
}
