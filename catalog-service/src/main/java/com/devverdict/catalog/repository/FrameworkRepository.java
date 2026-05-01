package com.devverdict.catalog.repository;

import com.devverdict.catalog.domain.Framework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FrameworkRepository extends JpaRepository<Framework, Long> {

    @Modifying
    @Query("UPDATE Framework f SET f.averageRating = (f.averageRating * f.reviewCount + :rating) / (f.reviewCount + 1), f.reviewCount = f.reviewCount + 1 WHERE f.id = :frameworkId")
    int updateAverageRating(@Param("frameworkId") Long frameworkId, @Param("rating") Integer rating);

    @Query("SELECT f FROM Framework f " +
           "WHERE (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:type IS NULL OR f.type = :type) " +
           "AND (:minRating IS NULL OR f.averageRating >= :minRating) " +
           "ORDER BY CASE " +
           "  WHEN :name IS NOT NULL AND LOWER(f.name) = LOWER(:name) THEN 0 " +
           "  WHEN :name IS NOT NULL AND LOWER(f.name) LIKE LOWER(CONCAT(:name, '%')) THEN 1 " +
           "  ELSE 2 " +
           "END, f.name")
    List<Framework> searchFrameworks(
            @Param("name") String name,
            @Param("type") String type,
            @Param("minRating") Double minRating);

    @Query("SELECT DISTINCT f.type FROM Framework f ORDER BY f.type")
    List<String> findDistinctTypes();
}
