package com.instituteops.audit.repo;

import com.instituteops.audit.domain.DataAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataAccessLogRepository extends JpaRepository<DataAccessLogEntity, Long> {
}
