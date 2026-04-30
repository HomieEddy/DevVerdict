package com.devverdict.catalog.repository;

import com.devverdict.catalog.domain.Framework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrameworkRepository extends JpaRepository<Framework, Long> {
}
