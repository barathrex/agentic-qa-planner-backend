package com.qaassistant.repository;

import com.qaassistant.entity.QaPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QaPlanRepository extends JpaRepository<QaPlan, Long> {
}
