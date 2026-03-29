package com.instituteops.security.repo;

import com.instituteops.security.domain.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.UserRoleId> {
}
