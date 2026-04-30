package com.devverdict.catalog.repository;

import com.devverdict.catalog.domain.Framework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FrameworkRepository extends JpaRepository<Framework, Long> {

    @Modifying
    @Query("UPDATE Framework f SET f.averageRating = (f.averageRating * f.reviewCount + :rating) / (f.reviewCount + 1), f.reviewCount = f.reviewCount + 1 WHERE f.id = :frameworkId")
    int updateAverageRating(@Param("frameworkId") Long frameworkId, @Param("rating") Integer rating);
}
