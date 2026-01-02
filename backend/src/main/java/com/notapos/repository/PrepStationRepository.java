package com.notapos.repository;

import com.notapos.entity.PrepStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PrepStation entity.
 * 
 * @author CJ
 */

@Repository
public interface PrepStationRepository extends JpaRepository<PrepStation, Long> {
    
    Optional<PrepStation> findByName(String name);              // Find prep station by name

    List<PrepStation> findByIsActive(Boolean isActive);         // Find all active prep stations
}
