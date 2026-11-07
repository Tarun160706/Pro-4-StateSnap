package com.statesnap.repository;

import com.statesnap.entity.RecoveryAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryAnalysisRepository extends JpaRepository<RecoveryAnalysis, String> {
    Optional<RecoveryAnalysis> findFirstByTransactionIdOrderByAnalyzedAtDesc(String transactionId);
    List<RecoveryAnalysis> findByTransactionIdOrderByAnalyzedAtDesc(String transactionId);
    void deleteByTransactionId(String transactionId);
}

