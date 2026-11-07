package com.statesnap.repository;

import com.statesnap.entity.TransactionEvent;
import com.statesnap.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, String> {

    List<TransactionEvent> findByTransactionOrderByTimestampAsc(TransactionRecord transaction);

    @Query("SELECT e FROM TransactionEvent e WHERE e.transaction.id = :txId " +
           "ORDER BY CASE WHEN e.sequenceNumber IS NOT NULL THEN e.sequenceNumber ELSE 999999999 END ASC, e.timestamp ASC")
    List<TransactionEvent> findByTransactionIdOrdered(@Param("txId") String transactionId);

    List<TransactionEvent> findByTransactionId(String transactionId);

    boolean existsByTransactionIdAndEventId(String transactionId, String eventId);

    long countByTransactionId(String transactionId);
}

