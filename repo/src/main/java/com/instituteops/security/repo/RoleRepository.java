package com.instituteops.security.repo;

import com.instituteops.security.domain.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleCode(String roleCode);
}
