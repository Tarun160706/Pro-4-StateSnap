package com.statesnap.repository;

import com.statesnap.entity.ReplayExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplayExecutionRepository extends JpaRepository<ReplayExecution, String> {
    List<ReplayExecution> findByOriginalTransactionIdOrderByCreatedAtDesc(String originalTransactionId);
}

