package com.qaassistant.repository;

import com.qaassistant.entity.QaPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QaPlanRepository extends JpaRepository<QaPlan, Long> {
    List<QaPlan> findByDeveloperNameOrderByCreatedDateDesc(String developerName);

    @Query("SELECT q FROM QaPlan q WHERE q.developerName = :developerName AND " +
           "(LOWER(q.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(q.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(q.developerName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<QaPlan> searchDeveloperPlans(@Param("developerName") String developerName, @Param("query") String query);
}
