package com.virtualbank.logging.repository;

import com.virtualbank.logging.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {
}