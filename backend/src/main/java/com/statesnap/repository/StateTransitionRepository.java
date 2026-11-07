package com.statesnap.repository;

import com.statesnap.entity.StateTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StateTransitionRepository extends JpaRepository<StateTransition, String> {
    List<StateTransition> findByTransactionIdOrderByTimestampAsc(String transactionId);
    void deleteByTransactionId(String transactionId);
}

