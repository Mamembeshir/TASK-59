package com.instituteops.security.repo;

import com.instituteops.security.domain.InternalApiClientEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternalApiClientRepository extends JpaRepository<InternalApiClientEntity, Long> {

    Optional<InternalApiClientEntity> findByClientKeyAndActiveTrue(String clientKey);
}
