package com.mutuelle.repository;

import com.mutuelle.entity.SettingsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingsHistoryRepository extends JpaRepository<SettingsHistory, Long> {
    List<SettingsHistory> findBySettingNameOrderByModifiedDateDesc(String settingName);
}
