package com.statesnap.repository;

import com.statesnap.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    Optional<TransactionRecord> findByExternalId(String externalId);

    Page<TransactionRecord> findByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE " +
           "LOWER(t.externalId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.channel) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TransactionRecord> searchByKeyword(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE t.status = :status AND (" +
           "LOWER(t.externalId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.channel) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TransactionRecord> searchByStatusAndKeyword(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(String status);
}
