package com.statesnap.repository;

import com.statesnap.entity.ReplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplayEventRepository extends JpaRepository<ReplayEvent, String> {
    List<ReplayEvent> findByReplayExecutionIdOrderBySimulatedSequenceNumberAscSimulatedTimestampAsc(String replayExecutionId);
}

