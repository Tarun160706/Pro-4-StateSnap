package com.statesnap.repository;

import com.statesnap.entity.EvidenceRecord;
import com.statesnap.entity.EvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRecordRepository extends JpaRepository<EvidenceRecord, String> {
    List<EvidenceRecord> findByTransactionIdOrderByDetectedAtAsc(String transactionId);
    List<EvidenceRecord> findByTransactionIdAndEvidenceType(String transactionId, EvidenceType evidenceType);
    void deleteByTransactionId(String transactionId);
}

