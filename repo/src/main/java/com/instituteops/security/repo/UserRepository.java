package com.instituteops.security.repo;

import com.instituteops.security.domain.UserEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);

    @Query("select u.id from UserEntity u where u.username = :username and u.deletedAt is null")
    Optional<Long> findIdByUsername(@Param("username") String username);

    @Modifying
    @Query("update UserEntity u set u.lastLoginAt = :lastLoginAt where u.username = :username")
    int updateLastLogin(@Param("username") String username, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
