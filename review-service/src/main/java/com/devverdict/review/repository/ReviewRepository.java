package com.devverdict.review.repository;

import com.devverdict.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByFrameworkIdOrderByCreatedAtDesc(Long frameworkId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
}
