package com.statesnap.repository;

import com.statesnap.entity.InferredTransactionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InferredTransactionStateRepository extends JpaRepository<InferredTransactionState, String> {
    Optional<InferredTransactionState> findFirstByTransactionIdOrderByReconstructedAtDesc(String transactionId);
    List<InferredTransactionState> findByTransactionIdOrderByReconstructedAtDesc(String transactionId);
    void deleteByTransactionId(String transactionId);
}

