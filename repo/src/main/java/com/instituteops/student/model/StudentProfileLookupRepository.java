package com.instituteops.student.model;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileLookupRepository extends JpaRepository<StudentProfileEntity, Long> {

    Optional<StudentProfileEntity> findByStudentNoIgnoreCaseAndDeletedAtIsNull(String studentNo);

    List<StudentProfileEntity> findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();
}
