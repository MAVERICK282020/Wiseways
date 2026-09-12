package com.wiseways.repository;

import com.wiseways.model.CollegeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollegeEntityRepository extends JpaRepository<CollegeEntity, Long> {

    /**
     * Nearest colleges by absolute closing-rank distance — powers the
     * fallback path when no eligible college is found for a rank.
     */
    @Query("SELECT c FROM CollegeEntity c ORDER BY ABS(c.closingRank - :rank)")
    List<CollegeEntity> findNearestByClosingRank(@Param("rank") double rank, Pageable pageable);
}
