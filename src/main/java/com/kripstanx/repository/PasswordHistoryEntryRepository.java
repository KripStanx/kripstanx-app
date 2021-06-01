package com.kripstanx.repository;

import com.kripstanx.domain.PasswordHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryEntryRepository extends JpaRepository<PasswordHistoryEntry, Long> {

    List<PasswordHistoryEntry> findByUsername(String username);

    List<PasswordHistoryEntry> findByUsernameOrderByEntryDateDesc(String username);
}
